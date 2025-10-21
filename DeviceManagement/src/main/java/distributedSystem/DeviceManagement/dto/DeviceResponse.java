package distributedSystem.DeviceManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceResponse {
    private Long id;
    private String name;
    private int maximConsumptionValue;
    private Long userId; // to client
}
