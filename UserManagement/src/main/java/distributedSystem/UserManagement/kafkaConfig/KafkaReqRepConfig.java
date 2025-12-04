package distributedSystem.UserManagement.kafkaConfig;

// src/main/java/distributedSystem/UserManagement/kafka/KafkaReqRepConfig.java

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaReqRepConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrap;

    // ---- Producer/Template for Map replies (distinct bean names!) ----
    @Bean("mapProducerFactory")
    public ProducerFactory<String, Map<String,Object>> mapProducerFactory() {
        Map<String,Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        p.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // reply as plain JSON
        return new DefaultKafkaProducerFactory<>(p);
    }

    @Bean("mapKafkaTemplate")
    public KafkaTemplate<String, Map<String,Object>> mapKafkaTemplate(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            @org.springframework.beans.factory.annotation.Qualifier("mapProducerFactory")
            ProducerFactory<String, Map<String,Object>> pf) {
        return new KafkaTemplate<>(pf);
    }

    // ---- Consumer/Factory for Map commands (distinct bean names!) ----
    @Bean("mapConsumerFactory")
    public ConsumerFactory<String, Map<String,Object>> mapConsumerFactory() {
        Map<String,Object> c = new HashMap<>();
        c.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        c.put(ConsumerConfig.GROUP_ID_CONFIG, "user-service-consumers");
        c.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        c.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        c.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        c.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        c.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        c.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.LinkedHashMap");
        return new DefaultKafkaConsumerFactory<>(c);
    }

    // ---- Listener factory wired to the Map reply template ----
    @Bean("mapListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Map<String,Object>> mapListenerFactory(
            @org.springframework.beans.factory.annotation.Qualifier("mapConsumerFactory")
            ConsumerFactory<String, Map<String,Object>> cf,
            @org.springframework.beans.factory.annotation.Qualifier("mapKafkaTemplate")
            KafkaTemplate<String, Map<String,Object>> replyTemplate) {

        var f = new ConcurrentKafkaListenerContainerFactory<String, Map<String,Object>>();
        f.setConsumerFactory(cf);
        // REQUIRED so correlation id is propagated in the reply
        f.setReplyTemplate(replyTemplate);
        return f;
    }
}

