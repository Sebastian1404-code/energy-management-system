package distributedSystem.CustomerSupport.repository;


import distributedSystem.CustomerSupport.Domain.Message;
import java.util.*;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findByConversationId(UUID conversationId);
}
