package distributedSystem.DeviceManagement.events;


import distributedSystem.DeviceManagement.dto.KafkaPayloadMonitoring;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
public class DeviceEventsProducer {

    private static final String TOPIC = "device.monitoring";
    private final KafkaTemplate<String, KafkaPayloadMonitoring> kafka;

    public DeviceEventsProducer(KafkaTemplate<String,KafkaPayloadMonitoring > kafka) {
        this.kafka = kafka;
    }

    public void publishDeviceCreatedEvent(Long deviceId,Long userId,int maximConsumptionValue) {
        KafkaPayloadMonitoring kafkaPayload=new KafkaPayloadMonitoring(deviceId,userId,maximConsumptionValue,DevMonType.DeviceCreated);
        kafka.send(TOPIC, deviceId.toString(), kafkaPayload);
        System.out.println("[Kafka] Published deviceId -> " + deviceId);
    }
    public void publishDeviceDeletedEvent(Long deviceId,Long userId,int maximConsumptionValue) {
        KafkaPayloadMonitoring kafkaPayload = new KafkaPayloadMonitoring(deviceId, userId,maximConsumptionValue, DevMonType.DeviceDeleted);
        kafka.send(TOPIC, deviceId.toString(), kafkaPayload);
        System.out.println("[Kafka] Published DELETED event deviceId -> " + kafkaPayload);
    }
}
