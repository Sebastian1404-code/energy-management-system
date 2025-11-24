package distributedSystem.UserManagement.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@EnableKafka
@Configuration
public class UserServiceKafkaConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String,Object>> kListenerFactory(
            org.springframework.kafka.core.ConsumerFactory<String, Map<String,Object>> cf,
            org.springframework.kafka.core.KafkaTemplate<String, Map<String,Object>> replyTemplate) {

        var f = new ConcurrentKafkaListenerContainerFactory<String, Map<String,Object>>();
        f.setConsumerFactory(cf);
        f.setReplyTemplate(replyTemplate);
        return f;
    }

    @Bean
    public KafkaTemplate<String, Map<String,Object>> replyTemplate(
            org.springframework.kafka.core.ProducerFactory<String, Map<String,Object>> pf) {
        return new org.springframework.kafka.core.KafkaTemplate<>(pf);
    }
}



