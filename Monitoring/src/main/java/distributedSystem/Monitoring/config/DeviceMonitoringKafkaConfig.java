package distributedSystem.Monitoring.config;

import distributedSystem.Monitoring.model.KafkaPayloadMonitoring;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class DeviceMonitoringKafkaConfig {

    @Bean
    public ConsumerFactory<String, KafkaPayloadMonitoring> kafkaPayloadMonitoringConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<KafkaPayloadMonitoring> valueDeserializer =
                new JsonDeserializer<>(KafkaPayloadMonitoring.class);
        valueDeserializer.addTrustedPackages(
                "distributedSystem.Monitoring.model",
                "distributedSystem.DeviceManagement.model"
        );
        // IMPORTANT: ignore type headers from the producer
        valueDeserializer.setUseTypeMapperForKey(false);
        valueDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, KafkaPayloadMonitoring>
    kafkaPayloadMonitoringListenerContainerFactory(
            ConsumerFactory<String, KafkaPayloadMonitoring> kafkaPayloadMonitoringConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, KafkaPayloadMonitoring> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaPayloadMonitoringConsumerFactory);
        return factory;
    }
}
