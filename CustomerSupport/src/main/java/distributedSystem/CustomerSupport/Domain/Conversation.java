package distributedSystem.CustomerSupport.Domain;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    private UUID id;
    private String userId;         // owner (client)
    private Instant createdAt;
    private Instant updatedAt;
}
