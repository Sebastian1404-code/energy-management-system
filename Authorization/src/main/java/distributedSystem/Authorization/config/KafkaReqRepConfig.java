package distributedSystem.Authorization.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaReqRepConfig {

    public static final String COMMAND_TOPIC = "user.commands.create-or-get";
    public static final String REPLY_TOPIC   = "user.replies.create-or-get";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Map<String, Object>> pf() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Optional: don’t add type headers (since we consume as Map)
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, Map<String, Object>> replyCf() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-service-user-create-replies");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.LinkedHashMap");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Map<String, Object>> repliesContainer(
            ConsumerFactory<String, Map<String, Object>> replyCf) {
        ContainerProperties containerProps = new ContainerProperties(REPLY_TOPIC);
        return new ConcurrentMessageListenerContainer<>(replyCf, containerProps);
    }

    @Bean
    public ReplyingKafkaTemplate<String, Map<String, Object>, Map<String, Object>> rrTemplate(
            ProducerFactory<String, Map<String, Object>> pf,
            ConcurrentMessageListenerContainer<String, Map<String, Object>> repliesContainer) {
        return new ReplyingKafkaTemplate<>(pf, repliesContainer);
    }
}
