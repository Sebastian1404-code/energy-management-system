package distributedSystem.Authorization.dto;

import distributedSystem.Authorization.dto.Role;
import lombok.Data;


@Data
public class CredentialRequest {
    private Long userId;
    private String username;
    private String password;
    private Role role;
}
