package distributedSystem.Authorization.events;


import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import distributedSystem.Authorization.config.KafkaReqRepConfig;
import distributedSystem.Authorization.dto.Role;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Component
public class UserKafkaGateway {

    private final ReplyingKafkaTemplate<String, Map<String, Object>, Map<String, Object>> rr;

    public UserKafkaGateway(ReplyingKafkaTemplate<String, Map<String, Object>, Map<String, Object>> rr) {
        this.rr = rr;
    }

    /**
     * Sends {"username","email","role": Role} and expects reply:
     * {"status":"OK|VALIDATION_ERROR|TEMPORARY_ERROR","userId":123?, "message": "..."}.
     */
    public Long createOrGetUserId(String username, String email, Role role) {
        // Role is sent as enum -> serialized automatically as "ADMIN", etc.
        Map<String, Object> cmd = Map.of(
                "username", username,
                "email", email,
                "role", role
        );

        ProducerRecord<String, Map<String, Object>> rec =
                new ProducerRecord<>(KafkaReqRepConfig.COMMAND_TOPIC, username, cmd);

        rec.headers().add(new RecordHeader(
                KafkaHeaders.REPLY_TOPIC,
                KafkaReqRepConfig.REPLY_TOPIC.getBytes(StandardCharsets.UTF_8))
        );

        try {
            RequestReplyFuture<String, Map<String, Object>, Map<String, Object>> fut = rr.sendAndReceive(rec);
            ConsumerRecord<String, Map<String, Object>> replyRec = fut.get(5, TimeUnit.SECONDS);
            Map<String, Object> reply = replyRec.value();

            if (reply == null)
                throw new IllegalStateException("Null reply from user service");

            String status = String.valueOf(reply.get("status"));
            if ("OK".equals(status)) {
                Object uid = reply.get("userId");
                if (uid == null)
                    throw new IllegalStateException("OK without userId");
                return (uid instanceof Number n) ? n.longValue() : Long.parseLong(uid.toString());
            }

            String message = String.valueOf(reply.get("message"));
            if ("VALIDATION_ERROR".equals(status)) {
                throw new IllegalArgumentException(message);
            }

            throw new IllegalStateException("User service error: " + status + " - " + message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get userId via Kafka", e);
        }
    }
}