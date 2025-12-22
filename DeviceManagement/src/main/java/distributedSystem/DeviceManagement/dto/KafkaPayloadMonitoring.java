package distributedSystem.DeviceManagement.dto;


import distributedSystem.DeviceManagement.events.DevMonType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KafkaPayloadMonitoring {
    private Long deviceId;
    private DevMonType devMonType;
}
