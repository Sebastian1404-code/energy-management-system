package distributedSystem.Authorization.dto;


import distributedSystem.Authorization.dto.Role;

public record CreateOrGetUserCommand(String username, String email, Role role) {}