package distributedSystem.Authorization.dto;

public record CreateUserRequest(String username, String email, Role role) {}
