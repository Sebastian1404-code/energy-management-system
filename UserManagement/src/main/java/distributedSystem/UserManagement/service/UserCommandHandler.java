package distributedSystem.UserManagement.service;

import distributedSystem.UserManagement.model.Role;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserCommandHandler {

    private final UserService userService;

    public UserCommandHandler(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(
            topics = "user.commands.create-or-get",
            containerFactory = "mapListenerFactory"
    )
    @SendTo("user.replies.create-or-get")
    public Map<String, Object> handle(Map<String, Object> cmd) {
        try {
            String username = String.valueOf(cmd.get("username"));
            String email    = String.valueOf(cmd.get("email"));

            // --- Parse Role as ENUM (case-insensitive) ---
            Object roleObj = cmd.get("role");
            if (roleObj == null) {
                throw new IllegalArgumentException("Missing 'role'");
            }

            Role role;
            if (roleObj instanceof String s) {
                try {
                    role = Role.valueOf(s.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid role: " + s);
                }
            } else if (roleObj instanceof Role r) {
                // In case the payload was deserialized directly as enum (unlikely with Map)
                role = r;
            } else {
                throw new IllegalArgumentException("Invalid 'role' type: " + roleObj.getClass().getSimpleName());
            }
            // ---------------------------------------------

            Long userId = userService.createIfAbsentAndReturnId(username, email, role);

            return Map.of("status", "OK", "userId", userId, "message", "OK");

        } catch (IllegalArgumentException ve) {
            return Map.of("status", "VALIDATION_ERROR", "message", ve.getMessage());

        } catch (Exception ex) {
            return Map.of("status", "TEMPORARY_ERROR", "message", ex.getMessage());
        }
    }
}