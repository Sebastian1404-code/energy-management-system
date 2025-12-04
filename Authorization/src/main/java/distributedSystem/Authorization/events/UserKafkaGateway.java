package distributedSystem.Authorization.events;

import distributedSystem.Authorization.config.KafkaReqRepConfig;
import distributedSystem.Authorization.dto.Role;
import distributedSystem.Authorization.dto.CreateOrGetUserStatus;
import distributedSystem.Authorization.dto.CreateOrGetUserCommand;
import distributedSystem.Authorization.dto.CreateOrGetUserReply;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class UserKafkaGateway {

    private final ReplyingKafkaTemplate<String, CreateOrGetUserCommand, CreateOrGetUserReply> rr;

    public UserKafkaGateway(ReplyingKafkaTemplate<String, CreateOrGetUserCommand, CreateOrGetUserReply> rr) {
        this.rr = rr;
    }

    public Long createOrGetUserId(String username, String email, Role role) {
        CreateOrGetUserCommand cmd = new CreateOrGetUserCommand(username, email, role);

        ProducerRecord<String, CreateOrGetUserCommand> rec =
                new ProducerRecord<>(KafkaReqRepConfig.COMMAND_TOPIC, username, cmd);

        rec.headers().add(new RecordHeader(
                KafkaHeaders.REPLY_TOPIC,
                KafkaReqRepConfig.REPLY_TOPIC.getBytes(StandardCharsets.UTF_8))
        );

        try {
            RequestReplyFuture<String, CreateOrGetUserCommand, CreateOrGetUserReply> fut = rr.sendAndReceive(rec);
            ConsumerRecord<String, CreateOrGetUserReply> replyRec = fut.get(10, TimeUnit.SECONDS);
            CreateOrGetUserReply reply = replyRec.value();

            if (reply == null) {
                throw new IllegalStateException("Null reply from user service");
            }

            if (reply.status() == CreateOrGetUserStatus.OK) {
                if (reply.userId() == null) {
                    throw new IllegalStateException("OK without userId");
                }
                return reply.userId();
            }

            if (reply.status() == CreateOrGetUserStatus.VALIDATION_ERROR) {
                throw new IllegalArgumentException(reply.message());
            }

            throw new IllegalStateException("User service error: " + reply.status() + " - " + reply.message());

        } catch (Exception e) {
            throw new RuntimeException("Failed to get userId via Kafka", e);
        }
    }
}
