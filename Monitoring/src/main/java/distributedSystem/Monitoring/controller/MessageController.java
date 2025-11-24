// src/main/java/distributedSystem/Monitoring/controller/MessageController.java
package distributedSystem.Monitoring.controller;

import distributedSystem.Monitoring.dto.DeviceSummaryDto;
import distributedSystem.Monitoring.model.WindowConsumption;
import distributedSystem.Monitoring.repository.WindowConsumptionRepository;
import distributedSystem.Monitoring.service.LastSeenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitoring")
public class MessageController {

    private final WindowConsumptionRepository windowRepo;

    // configurable formatting for numeric device id -> "device-001"
    private final String devicePrefix;
    private final int idPad;
    private final LastSeenService lastSeen;


    public MessageController(
            WindowConsumptionRepository windowRepo,
            @Value("${app.device-prefix:device-}") String devicePrefix,
            @Value("${app.device-id-pad:3}") int idPad, LastSeenService lastSeen
    ) {
        this.windowRepo = windowRepo;
        this.devicePrefix = devicePrefix;
        this.idPad = Math.max(0, idPad);
        this.lastSeen = lastSeen;
    }




    @GetMapping
    public List<DeviceSummaryDto> list() {
        List<WindowConsumption> rows = windowRepo.findLatestWindowPerDeviceNative(); // or native variant
        return rows.stream().map(w -> {
            Instant last = lastSeen.get(w.getDeviceId());
            if (last == null) last = w.getUpdatedAtUtc(); // fallback after restart
            return new DeviceSummaryDto(
                    w.getDeviceId(),
                    w.getWindowStartUtc(),
                    w.getWindowMinutes(),
                    w.getKwh(),
                    w.getSampleCount(),
                    last
            );
        }).toList();
    }

    @GetMapping("/devices/{deviceId}/series")
    public List<Map<String, Object>> rawSeriesForDay(
            @PathVariable String deviceId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "UTC") String tz,
            @RequestParam(name = "virtualHourMinutes", defaultValue = "#{${app.aggregate-minutes:60}}")
            int virtualHourMinutes
    ) {
        String id = normalizeDeviceId(deviceId);

        ZoneId zone = ZoneId.of(tz);
        LocalDate localDay = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now(zone);
        ZonedDateTime zStart = localDay.atStartOfDay(zone);
        ZonedDateTime zEnd = zStart.plusDays(1);
        Instant startUtc = zStart.toInstant();
        Instant endUtc = zEnd.toInstant();

        List<Object[]> rows = windowRepo.findRawWindowsForDay(id, startUtc, endUtc);

        long startMs = startUtc.toEpochMilli();
        long unitMs = Math.max(1, virtualHourMinutes) * 60_000L; // virtual “hour” = X minutes

        List<Map<String, Object>> simplifiedPoints = new ArrayList<>(rows.size());

        for (Object[] r : rows) {
            Instant ts = toInstant(r[0]);
            double kwh = ((Number) r[1]).doubleValue();
            int windowMin = ((Number) r[3]).intValue();

            long tsMs = ts.toEpochMilli();
            double xVirt = (tsMs - startMs) / (double) unitMs; // virtual position in day

            // extract hour and minute for frontend display
            ZonedDateTime localTs = ts.atZone(zone);
            int hour = localTs.getHour();
            int minute = localTs.getMinute();

            simplifiedPoints.add(Map.of(
                    "hour", hour,
                    "minute", minute,
                    "ts_utc", ts.toString(),
                    "kwh", kwh,
                    "x_virtual_hour", xVirt
            ));
        }

        return simplifiedPoints;
    }

    private String normalizeDeviceId(String deviceId) {
        // if it's purely digits, map to device-XXX with zero padding
        if (deviceId.matches("\\d+")) {
            int n = Integer.parseInt(deviceId);
            return devicePrefix + String.format("%0" + idPad + "d", n); // e.g., device-001
        }
        return deviceId; // already a full id like device-001
    }

    private static Instant toInstant(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.Instant i) return i;
        if (v instanceof java.sql.Timestamp t) return t.toInstant();
        if (v instanceof java.util.Date d) return d.toInstant();
        if (v instanceof CharSequence s) return Instant.parse(s.toString());
        throw new IllegalArgumentException("Unsupported time type: " + v.getClass());
    }


}
