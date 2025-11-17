package distributedSystem.UserManagement.dto;

import distributedSystem.UserManagement.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class CredentialRequest {
    private Long userId;
    private String username;
    private String password;
    private Role role;
}
