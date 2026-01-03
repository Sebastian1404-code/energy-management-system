package distributedSystem.Monitoring.kafka;


import distributedSystem.Monitoring.dto.OverconsumptionAlertDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertProducer {
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public AlertProducer(KafkaTemplate<String, Object> kafka,
                         @Value("${app.alert-topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public void send(OverconsumptionAlertDto alert) {
        kafka.send(topic, String.valueOf(alert.deviceId()), alert);
    }
}
