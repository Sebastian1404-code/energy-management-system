package distributedSystem.UserManagement.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventsProducer {

    private static final String TOPIC = "user.created.v1";
    private final KafkaTemplate<String, Long> kafka;  // <String, Long>

    public UserEventsProducer(KafkaTemplate<String, Long> kafka) {
        this.kafka = kafka;
    }

    public void publishUserCreatedEvent(Long userId) {
        kafka.send(TOPIC, userId.toString(), userId);
        System.out.println("[Kafka] Published userId -> " + userId);
    }
}
