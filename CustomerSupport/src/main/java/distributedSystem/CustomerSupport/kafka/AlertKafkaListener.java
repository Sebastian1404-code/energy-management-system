package distributedSystem.CustomerSupport.kafka;

import distributedSystem.CustomerSupport.dto.OverconsumptionAlertDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AlertKafkaListener {

    private final SimpMessagingTemplate messaging;

    @KafkaListener(topics = "${app.alert-topic}", groupId = "ws-alerts")
    public void onAlert(OverconsumptionAlertDto alert) {
        if (alert == null || alert.userId() == null) return;

        messaging.convertAndSend("/topic/alerts.user." + alert.userId(), alert);
    }
}

