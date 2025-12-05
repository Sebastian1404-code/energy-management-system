package distributedSystem.Monitoring.events;

import distributedSystem.Monitoring.model.KafkaPayloadMonitoring;
import distributedSystem.Monitoring.service.DeviceRefService;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class DeviceEventsConsumer {
    private final DeviceRefService deviceRefService;

    public DeviceEventsConsumer(DeviceRefService deviceRefService) {
        this.deviceRefService= deviceRefService;
    }

    @Transactional
    @KafkaListener(topics = "device.monitoring", groupId = "monitoring-device-service",
            containerFactory = "kafkaPayloadMonitoringListenerContainerFactory"
    )
    public void consumeDeviceCreated(KafkaPayloadMonitoring kafkaPayloadMonitoring) {
        switch (kafkaPayloadMonitoring.getDevMonType()) {
            case DeviceCreated -> deviceRefService.handleDeviceCreated(kafkaPayloadMonitoring.getDeviceId());

            case DeviceDeleted -> deviceRefService.handleDeviceDeleted(kafkaPayloadMonitoring.getDeviceId());

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + kafkaPayloadMonitoring.getDevMonType()
            );
        }
    }
}
