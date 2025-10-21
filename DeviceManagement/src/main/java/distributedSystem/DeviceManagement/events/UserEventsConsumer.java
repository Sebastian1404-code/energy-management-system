package distributedSystem.DeviceManagement.events;

import distributedSystem.DeviceManagement.service.UserSyncService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventsConsumer {

    private final UserSyncService userSyncService;

    public UserEventsConsumer(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @KafkaListener(topics = "user.created.v1", groupId = "device-service")
    public void consumeUserCreated(Long userId) {
        userSyncService.handleUserCreated(userId);
    }
}
