package distributedSystem.Authorization.model;

import distributedSystem.Authorization.dto.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId generated/owned by User Service
    private Long userId;


    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    // BCrypt hash
    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Role role;
}

