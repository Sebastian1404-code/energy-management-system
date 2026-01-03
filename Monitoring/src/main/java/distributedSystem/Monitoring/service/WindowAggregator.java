// src/main/java/distributedSystem/Monitoring/service/WindowAggregator.java
package distributedSystem.Monitoring.service;

import distributedSystem.Monitoring.dto.DeviceSummaryDto;
import distributedSystem.Monitoring.dto.OverconsumptionAlertDto;
import distributedSystem.Monitoring.kafka.AlertProducer;
import distributedSystem.Monitoring.model.WindowConsumption;
import distributedSystem.Monitoring.repository.DeviceMonitoringRefRepository;
import distributedSystem.Monitoring.repository.WindowConsumptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WindowAggregator {

    /** Keyed by numeric device id to avoid "device-1" vs "device-001" splitting buckets. */
    public record Key(Long deviceId, Instant windowStartUtc) {}

    /**
     * Keeps window totals (for alerting) and deltas (for periodic DB flush).
     * One Bucket represents one (device, windowStart) pair.
     */
    public static final class Bucket {
        // running totals for the current window (used for alert decision)
        volatile double totalKwh;
        volatile int totalSamples;

        // delta accumulated since last flush (used for upsertAdd)
        volatile double pendingKwh;
        volatile int pendingSamples;

        // ensures only one alert per window
        volatile boolean alerted;

        Bucket(double firstKwh) {
            this.totalKwh = firstKwh;
            this.totalSamples = 1;
            this.pendingKwh = firstKwh;
            this.pendingSamples = 1;
            this.alerted = false;
        }

        void add(double v) {
            this.totalKwh += v;
            this.totalSamples += 1;
            this.pendingKwh += v;
            this.pendingSamples += 1;
        }

        /** Extract and reset deltas atomically under compute. */
        Delta drainPending() {
            Delta d = new Delta(this.pendingKwh, this.pendingSamples);
            this.pendingKwh = 0.0;
            this.pendingSamples = 0;
            return d;
        }
    }

    public record Delta(double kwh, int samples) {}

    private final WindowConsumptionRepository windowRepo;
    private final DeviceMonitoringRefRepository deviceMonitoringRefRepository;
    private final AlertProducer alertProducer;
    private final LastSeenService lastSeen;

    private final Map<Key, Bucket> buf = new ConcurrentHashMap<>();

    private final int windowMinutes;
    private final boolean useProcessingTime;
    private final String devicePrefix;
    private final int idPad;

    public WindowAggregator(
            WindowConsumptionRepository windowRepo,
            DeviceMonitoringRefRepository deviceMonitoringRefRepository,
            AlertProducer alertProducer,
            LastSeenService lastSeen,
            @Value("${app.aggregate-minutes:60}") int windowMinutes,
            @Value("${app.window.use-processing-time:false}") boolean useProcessingTime,
            @Value("${app.device-prefix:device-}") String devicePrefix,
            @Value("${app.device-id-pad:3}") int idPad
    ) {
        this.windowRepo = windowRepo;
        this.deviceMonitoringRefRepository = deviceMonitoringRefRepository;
        this.alertProducer = alertProducer;
        this.lastSeen = lastSeen;

        this.windowMinutes = Math.max(1, windowMinutes);
        this.useProcessingTime = useProcessingTime;
        this.devicePrefix = devicePrefix;
        this.idPad = Math.max(0, idPad);
    }

    private Instant windowStart(Instant basis) {
        long epochSec = basis.getEpochSecond();
        long sizeSec = windowMinutes * 60L;
        long start = (epochSec / sizeSec) * sizeSec;
        return Instant.ofEpochSecond(start);
    }

    /**
     * Called on every device reading.
     * - updates totals + pending deltas for this window
     * - triggers alert immediately once totalKwh > threshold (if DeviceMonitoringRef exists)
     */
    public void add(String deviceIdStr, Instant eventTimestampUtc, double valueKwh) {
        Long deviceId = extractDeviceId(deviceIdStr);
        if (deviceId == null) return; // cannot map -> cannot alert, cannot aggregate reliably

        Instant basis = useProcessingTime ? Instant.now() : eventTimestampUtc;
        Instant win = windowStart(basis);
        Key key = new Key(deviceId, win);

        // Update bucket totals/deltas
        Bucket bucket = buf.compute(key, (k, b) -> {
            if (b == null) return new Bucket(valueKwh);
            b.add(valueKwh);
            return b;
        });

        // Alert logic (only if not already alerted in this window)
        // We keep this outside compute to avoid doing DB work while holding the map's per-key lock.
        if (!bucket.alerted) {
            deviceMonitoringRefRepository.findById(deviceId).ifPresent(ref -> {
                int threshold = ref.getMaximConsumptionValue();

                // "instant": first moment the running total becomes strictly greater than threshold
                if (!bucket.alerted && bucket.totalKwh > threshold) {
                    bucket.alerted = true;

                    alertProducer.send(new OverconsumptionAlertDto(
                            ref.getUserId(),
                            ref.getDevice_id(),
                            win,
                            windowMinutes,
                            bucket.totalKwh,
                            threshold,
                            Instant.now()
                    ));
                }
            });
            // If ref missing -> do nothing (no alert), as requested.
        }
    }

    /**
     * Flush pending deltas to DB periodically.
     * Keeps in-memory totals so alerting can work across multiple flush cycles.
     */
    @Scheduled(fixedDelayString = "#{${app.flush-seconds:5} * 1000}")
    @Transactional
    public void flush() {
        if (buf.isEmpty()) return;

        Instant now = Instant.now();

        // Drain deltas per key and write to DB
        buf.forEach((k, ignored) -> {
            final Delta[] drained = new Delta[1];

            buf.computeIfPresent(k, (key, b) -> {
                drained[0] = b.drainPending();
                return b;
            });

            Delta d = drained[0];
            if (d != null && d.samples() > 0) {
                windowRepo.upsertAdd(
                        normalizeDeviceId(String.valueOf(k.deviceId())),  // DB uses string device ids (device-001)
                        k.windowStartUtc(),
                        windowMinutes,
                        d.kwh(),
                        d.samples(),
                        now
                );
            }
        });

        // Cleanup: keep only recent windows in memory (prevents growth)
        // Keep current + previous window (2 windows) to be safe.
        Instant cutoff = now.minusSeconds(windowMinutes * 60L * 2L);
        Instant cutoffWindow = windowStart(cutoff);
        buf.keySet().removeIf(key -> key.windowStartUtc().isBefore(cutoffWindow));
    }

    // ====== Reporting methods you already had ======

    public List<Map<String, Object>> buildRawSeriesForDay(
            String deviceId, String date, String tz, int virtualHourMinutes
    ) {
        String id = normalizeDeviceId(deviceId);

        ZoneId zone = ZoneId.of(tz);
        LocalDate localDay = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now(zone);
        ZonedDateTime zStart = localDay.atStartOfDay(zone);
        ZonedDateTime zEnd = zStart.plusDays(1);
        Instant startUtc = zStart.toInstant();
        Instant endUtc = zEnd.toInstant();

        List<Object[]> rows = windowRepo.findRawWindowsForDay(id, startUtc, endUtc);

        long startMs = startUtc.toEpochMilli();
        long unitMs = Math.max(1, virtualHourMinutes) * 60_000L;

        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Instant ts = toInstant(r[0]);
            double kwh = ((Number) r[1]).doubleValue();

            long tsMs = ts.toEpochMilli();
            double xVirt = (tsMs - startMs) / (double) unitMs;

            ZonedDateTime localTs = ts.atZone(zone);
            out.add(Map.of(
                    "hour", localTs.getHour(),
                    "minute", localTs.getMinute(),
                    "ts_utc", ts.toString(),
                    "kwh", kwh,
                    "x_virtual_hour", xVirt
            ));
        }
        return out;
    }

    public List<DeviceSummaryDto> listDeviceSummaries() {
        List<WindowConsumption> rows = windowRepo.findLatestWindowPerDeviceNative();

        return rows.stream()
                .map(w -> {
                    Instant last = lastSeen.get(w.getDeviceId());
                    if (last == null) last = w.getUpdatedAtUtc();
                    return new DeviceSummaryDto(
                            w.getDeviceId(),
                            w.getWindowStartUtc(),
                            w.getWindowMinutes(),
                            w.getKwh(),
                            w.getSampleCount(),
                            last
                    );
                })
                .toList();
    }

    // ====== helpers ======

    private static Instant toInstant(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        if (o instanceof java.util.Date d) return d.toInstant();
        throw new IllegalArgumentException("Unsupported timestamp type: " + o);
    }

    /**
     * Normalize numeric device id to the string format used in window_consumption (e.g. "device-001").
     * If the input already looks like "device-XXX", it will be returned as-is by callers that pass strings.
     */
    private String normalizeDeviceId(String deviceId) {
        Long id = extractDeviceId(deviceId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid deviceId: " + deviceId);
        }
        return devicePrefix + String.format("%0" + idPad + "d", id);
    }


    /**
     * Accepts "device-001", "001", "1" and extracts numeric id.
     */
    private Long extractDeviceId(String deviceIdStr) {
        if (deviceIdStr == null) return null;
        String digits = deviceIdStr.replaceAll("\\D+", "");
        return digits.isBlank() ? null : Long.parseLong(digits);
    }
}
