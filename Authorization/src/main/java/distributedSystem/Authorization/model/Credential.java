package distributedSystem.Authorization.model;

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

    @NotBlank
    @Column(nullable = false)
    private String role; // "CLIENT" | "ADMIN"
}

