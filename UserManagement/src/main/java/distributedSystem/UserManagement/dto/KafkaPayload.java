package distributedSystem.UserManagement.dto;

import distributedSystem.UserManagement.events.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class KafkaPayload {

    private Long userId;
    private EventType eventType;
}
