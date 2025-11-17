package distributedSystem.DeviceManagement.events;

import distributedSystem.DeviceManagement.dto.KafkaPayload;
import distributedSystem.DeviceManagement.service.UserSyncService;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventsConsumer {

    private final UserSyncService userSyncService;

    public UserEventsConsumer(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @Transactional
    @KafkaListener(topics = "user.created.v1", groupId = "device-service")
    public void consumeUserCreated(KafkaPayload kafkaPayload) {
        switch (kafkaPayload.getEventType()) {
            case UserCreated ->
                    userSyncService.handleUserCreated(kafkaPayload.getUserId());

            case UserDeleted ->
                    userSyncService.handleUserDeleted(kafkaPayload.getUserId());

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported event type: " + kafkaPayload.getEventType()
                    );
        }

    }
}
