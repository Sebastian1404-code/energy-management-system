package distributedSystem.Authorization.config;

import distributedSystem.Authorization.dto.CreateOrGetUserCommand;
import distributedSystem.Authorization.dto.CreateOrGetUserReply;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaReqRepConfig {

    public static final String COMMAND_TOPIC = "user.commands.create-or-get";
    public static final String REPLY_TOPIC   = "user.replies.create-or-get";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // -------- Producer: CreateOrGetUserCommand -> JSON --------
    @Bean
    public ProducerFactory<String, CreateOrGetUserCommand> pf() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // keep wire generic JSON (no type headers needed by the consumer)
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    // -------- Consumer: JSON -> CreateOrGetUserReply --------
    @Bean
    public ConsumerFactory<String, CreateOrGetUserReply> replyCf() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-service-user-create-replies");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Map reply JSON directly into our record class (no type headers from producer/User svc)
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "distributedSystem.Authorization.dto.CreateOrGetUserReply");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // -------- Listener container subscribed to the reply topic --------
    @Bean
    public ConcurrentMessageListenerContainer<String, CreateOrGetUserReply> repliesContainer(
            ConsumerFactory<String, CreateOrGetUserReply> replyCf) {
        ContainerProperties containerProps = new ContainerProperties(REPLY_TOPIC);
        return new ConcurrentMessageListenerContainer<>(replyCf, containerProps);
    }

    // -------- Typed ReplyingKafkaTemplate --------
    @Bean
    public ReplyingKafkaTemplate<String, CreateOrGetUserCommand, CreateOrGetUserReply> rrTemplate(
            ProducerFactory<String, CreateOrGetUserCommand> pf,
            ConcurrentMessageListenerContainer<String, CreateOrGetUserReply> repliesContainer) {
        ReplyingKafkaTemplate<String, CreateOrGetUserCommand, CreateOrGetUserReply> t =
                new ReplyingKafkaTemplate<>(pf, repliesContainer);
        // Helpful while testing; tweak as needed
        t.setDefaultReplyTimeout(Duration.ofSeconds(15));
        return t;
    }
}
