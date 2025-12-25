package distributedSystem.DeviceManagement.events;

import distributedSystem.DeviceManagement.dto.KafkaPayloadUser;
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
    public void consumeUserCreated(KafkaPayloadUser kafkaPayloadUser) {
        switch (kafkaPayloadUser.getEventType()) {
            case UserCreated ->
                    userSyncService.handleUserCreated(kafkaPayloadUser.getUserId());

            case UserDeleted ->
                    userSyncService.handleUserDeleted(kafkaPayloadUser.getUserId());

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported event type: " + kafkaPayloadUser.getEventType()
                    );
        }

    }
}
