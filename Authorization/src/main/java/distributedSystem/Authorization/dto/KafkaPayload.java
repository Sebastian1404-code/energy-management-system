package distributedSystem.Authorization.dto;

import distributedSystem.Authorization.events.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class KafkaPayload {

    private Long userId;
    private EventType eventType;


}

