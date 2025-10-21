package distributedSystem.DeviceManagement.dto;

import lombok.Data;

@Data
public class DeviceRequest {
    private String name;
    private int maximConsumptionValue;
    private Long userId;   // from client
}
