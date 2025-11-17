package distributedSystem.UserManagement.events;

import distributedSystem.UserManagement.dto.KafkaPayload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventsProducer {

    private static final String TOPIC = "user.created.v1";
    private final KafkaTemplate<String, KafkaPayload> kafka;

    public UserEventsProducer(KafkaTemplate<String,KafkaPayload > kafka) {
        this.kafka = kafka;
    }

    public void publishUserCreatedEvent(Long userId) {
        KafkaPayload kafkaPayload=new KafkaPayload(userId,EventType.UserCreated);
        kafka.send(TOPIC, userId.toString(), kafkaPayload);
        System.out.println("[Kafka] Published userId -> " + userId);
    }

    public void publishUserDeletedEvent(Long userId) {
        KafkaPayload kafkaPayload = new KafkaPayload(userId, EventType.UserDeleted);
        kafka.send(TOPIC, userId.toString(), kafkaPayload);
        System.out.println("[Kafka] Published DELETED event -> " + kafkaPayload);
    }
}
