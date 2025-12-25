package distributedSystem.CustomerSupport.repository;


import distributedSystem.CustomerSupport.Domain.Conversation;
import java.util.*;

public interface ConversationRepository {
    Optional<Conversation> findById(UUID id);
    Optional<Conversation> findByUserId(String userId);
    Conversation save(Conversation conversation);
    List<Conversation> findAll();
}
