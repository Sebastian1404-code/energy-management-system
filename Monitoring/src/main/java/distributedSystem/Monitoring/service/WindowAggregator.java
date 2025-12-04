// src/main/java/distributedSystem/Monitoring/service/WindowAggregator.java
package distributedSystem.Monitoring.service;

import distributedSystem.Monitoring.dto.DeviceSummaryDto;
import distributedSystem.Monitoring.model.WindowConsumption;
import distributedSystem.Monitoring.repository.WindowConsumptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WindowAggregator {

    public record Key(String deviceId, Instant windowStartUtc) {}
    public static final class Bucket {
        volatile double kwh;
        volatile int samples;
        Bucket(double kwh, int samples) { this.kwh = kwh; this.samples = samples; }
        void add(double v) { this.kwh += v; this.samples += 1; }
    }

    private final WindowConsumptionRepository windowRepo;

    private final Map<Key, Bucket> buf = new ConcurrentHashMap<>();
    private final long flushSeconds;
    private final int windowMinutes;
    private final boolean useProcessingTime;
    private final String devicePrefix;
    private final int idPad;
    private final LastSeenService lastSeen;



    public WindowAggregator(
            WindowConsumptionRepository windowRepo,
            @Value("${app.flush-seconds:5}") long flushSeconds,
            @Value("${app.aggregate-minutes:60}") int windowMinutes,
            @Value("${app.window.use-processing-time:false}") boolean useProcessingTime,
            @Value("${app.device-prefix:device-}") String devicePrefix,
            @Value("${app.device-id-pad:3}") int idPad, LastSeenService lastSeen

    ) {
        this.windowRepo = windowRepo;
        this.flushSeconds = flushSeconds;
        this.windowMinutes = Math.max(1, windowMinutes);
        this.useProcessingTime = useProcessingTime;
        this.devicePrefix=devicePrefix;
        this.idPad = Math.max(0, idPad);
        this.lastSeen = lastSeen;
    }

    private Instant windowStart(Instant basis) {
        long epochSec = basis.getEpochSecond();
        long sizeSec = windowMinutes * 60L;
        long start = (epochSec / sizeSec) * sizeSec;
        return Instant.ofEpochSecond(start);
    }

    public void add(String deviceId, Instant eventTimestampUtc, double valueKwh) {
        Instant basis = useProcessingTime ? Instant.now() : eventTimestampUtc;
        Instant win = windowStart(basis);
        Key key = new Key(deviceId, win);
        buf.compute(key, (k, b) -> {
            if (b == null) {
                return new Bucket(valueKwh, 1);
            }
            b.add(valueKwh);
            return b;
        });

    }

    @Scheduled(fixedDelayString = "#{${app.flush-seconds:5} * 1000}")
    @Transactional               // ← add this (or on the repo method, but I like both)
    public void flush() {
        if (buf.isEmpty()) return;
        Map<Key, Bucket> snapshot = new ConcurrentHashMap<>(buf);
        buf.clear();
        Instant now = Instant.now();
        snapshot.forEach((k, b) ->
                windowRepo.upsertAdd(k.deviceId(), k.windowStartUtc(), windowMinutes, b.kwh, b.samples, now)
        );
    }


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
        long unitMs = Math.max(1, virtualHourMinutes) * 60_000L; // virtual “hour” = X minutes

        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Instant ts = toInstant(r[0]);
            double kwh = ((Number) r[1]).doubleValue();
            int windowMin = ((Number) r[3]).intValue(); // kept for symmetry; not used below

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

    private static Instant toInstant(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        if (o instanceof java.util.Date d) return d.toInstant();
        throw new IllegalArgumentException("Unsupported timestamp type: " + o);
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId.matches("\\d+")) {
            int n = Integer.parseInt(deviceId);
            return devicePrefix + String.format("%0" + idPad + "d", n);
        }
        return deviceId;
    }


    public List<DeviceSummaryDto> listDeviceSummaries() {
        List<WindowConsumption> rows = windowRepo.findLatestWindowPerDeviceNative();

        return rows.stream()
                .map(w -> {
                    Instant last = lastSeen.get(w.getDeviceId());
                    if (last == null) {
                        last = w.getUpdatedAtUtc(); // fallback after restart
                    }
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
}
