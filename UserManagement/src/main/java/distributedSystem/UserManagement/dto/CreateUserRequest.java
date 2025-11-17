package distributedSystem.UserManagement.dto;

import distributedSystem.UserManagement.model.Role;
import lombok.Data;


@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private Role role;

}
