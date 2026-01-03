package distributedSystem.Monitoring.dto;


import java.time.Instant;

public record OverconsumptionAlertDto(
        Long userId,
        Long deviceId,
        Instant windowStartUtc,
        int windowMinutes,
        double kwhSoFar,
        int maxConsumptionValue,
        Instant createdAtUtc
) {}
