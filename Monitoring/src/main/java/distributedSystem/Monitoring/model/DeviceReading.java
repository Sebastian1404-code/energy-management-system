package distributedSystem.Monitoring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceReading(
        String device_id,
        String timestamp,
        double value_kwh
) {
}
