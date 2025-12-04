package distributedSystem.Monitoring.dto;

// src/main/java/distributedSystem/Monitoring/dto/LatestWindowResponse.java

import java.time.Instant;

public record LatestWindowResponse(
        String device_id,
        Instant window_start_utc,
        int window_minutes,
        double kwh,
        int sample_count
) {}
