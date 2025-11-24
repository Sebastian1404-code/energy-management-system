// src/main/java/distributedSystem/Monitoring/dto/DeviceSummaryDto.java
package distributedSystem.Monitoring.dto;

import java.time.Instant;

public record DeviceSummaryDto(
        String device_id,
        Instant latest_window_start_utc,
        int window_minutes,
        double kwh_in_latest_window,
        int sample_count_in_latest_window,
        Instant last_event_utc
) {}
