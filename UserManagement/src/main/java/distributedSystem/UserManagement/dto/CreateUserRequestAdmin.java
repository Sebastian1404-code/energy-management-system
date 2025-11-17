package distributedSystem.UserManagement.dto;

import distributedSystem.UserManagement.model.Role;
import lombok.Data;


@Data
public class CreateUserRequestAdmin {

    private String username;
    private String email;
    private String password;
    private Role role;
}
