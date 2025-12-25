package distributedSystem.CustomerSupport.repository;


import distributedSystem.CustomerSupport.Domain.Conversation;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryConversationRepository implements ConversationRepository {
    private final Map<UUID, Conversation> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<Conversation> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Conversation> findByUserId(String userId) {
        UUID id = byUserId.get(userId);
        if (id == null) return Optional.empty();
        return findById(id);
    }

    @Override
    public Conversation save(Conversation c) {
        byId.put(c.getId(), c);
        byUserId.put(c.getUserId(), c.getId());
        return c;
    }

    @Override
    public List<Conversation> findAll() {
        return new ArrayList<>(byId.values());
    }
}
