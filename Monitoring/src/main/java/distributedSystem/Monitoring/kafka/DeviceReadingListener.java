// src/main/java/distributedSystem/Monitoring/kafka/DeviceReadingListener.java
package distributedSystem.Monitoring.kafka;

import distributedSystem.Monitoring.model.DeviceReading;
import distributedSystem.Monitoring.service.LastSeenService;
import distributedSystem.Monitoring.service.WindowAggregator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeviceReadingListener {

    private static final Logger log = LoggerFactory.getLogger(DeviceReadingListener.class);
    private final ObjectMapper mapper;
    private final WindowAggregator aggregator;
    private final LastSeenService lastSeen;


    public DeviceReadingListener(ObjectMapper mapper, WindowAggregator aggregator, LastSeenService lastSeen) {
        this.mapper = mapper;
        this.aggregator = aggregator;
        this.lastSeen = lastSeen;
    }

    @KafkaListener(
            topics = "${app.device-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "filteredKafkaListenerContainerFactory"   // << use the filtered factory
    )
    public void onMessage(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            ConsumerRecord<String, String> record
    ) {
        try {
            if (payload == null) return;                 // extra safety
            String p = payload.trim();
            if (p.isEmpty() || p.charAt(0) != '{') {     // should be filtered already, but belt & suspenders
                log.warn("Skipping non-JSON payload @{} p{} off {}", topic, partition, offset);
                return;
            }

            DeviceReading dr = mapper.readValue(p, DeviceReading.class);


            Instant ts = Instant.parse(dr.timestamp());
            lastSeen.mark(dr.device_id(), ts);
            aggregator.add(dr.device_id(), ts, dr.value_kwh());

            if ((offset % 10) == 0) {
                log.info("Consumed {} p{}@{} key={} device={} ts={} v={}",
                        topic, partition, offset, record.key(), dr.device_id(), dr.timestamp(), dr.value_kwh());
            }
        } catch (Exception e) {
            log.warn("Failed to parse @{} p{} off {}: {} | payload={}",
                    topic, partition, offset, e.toString(), payload);
        }
    }
}
