package distributedSystem.CustomerSupport.repository;


import distributedSystem.CustomerSupport.Domain.Message;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryMessageRepository implements MessageRepository {
    private final Map<UUID, List<Message>> byConversation = new ConcurrentHashMap<>();

    @Override
    public Message save(Message message) {
        byConversation.computeIfAbsent(message.getConversationId(), k -> new CopyOnWriteArrayList<>())
                .add(message);
        return message;
    }

    @Override
    public List<Message> findByConversationId(UUID conversationId) {
        return new ArrayList<>(byConversation.getOrDefault(conversationId, List.of()));
    }
}
