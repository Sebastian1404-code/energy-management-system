package distributedSystem.DeviceManagement.dto;

import distributedSystem.DeviceManagement.events.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KafkaPayloadUser {

    private Long userId;
    private EventType eventType;
}
