// src/main/java/distributedSystem/Monitoring/config/KafkaListenerConfig.java
package distributedSystem.Monitoring.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerSimulationConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerSimulationConfig.class);

    @Bean(name = "filteredKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> filteredKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
        f.setConsumerFactory(consumerFactory);

        // Filter: drop non-JSON values early (true = filtered out)
        f.setRecordFilterStrategy(rec -> {
            String v = rec.value();
            if (v == null) return true;
            String p = v.trim();
            return p.isEmpty() || p.charAt(0) != '{';
        });

        // Commit offsets for filtered records so we don't see them again
        f.setAckDiscarded(true);

        // Error handler: 0 retries, then "recover" by logging and SKIP (advance offset)
        var backoff = new FixedBackOff(0L, 0L); // no retry
        DefaultErrorHandler eh = new DefaultErrorHandler(
                (ConsumerRecord<?, ?> rec, Exception ex) -> {
                    log.warn("Skipping bad record after 0 retries: topic={} partition={} offset={} | error={}",
                            rec.topic(), rec.partition(), rec.offset(), ex.toString());
                },
                backoff
        );
        f.setCommonErrorHandler(eh);

        return f;
    }
}
