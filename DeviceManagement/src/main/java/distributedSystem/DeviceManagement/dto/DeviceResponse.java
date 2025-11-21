package distributedSystem.DeviceManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Long id;
    private String name;
    private int maximConsumptionValue;
    private Long userId; // to client
}
