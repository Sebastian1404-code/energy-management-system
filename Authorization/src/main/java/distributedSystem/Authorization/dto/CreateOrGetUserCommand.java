package distributedSystem.Authorization.dto;

public record CreateOrGetUserCommand(String username, String email, Role role) {}
