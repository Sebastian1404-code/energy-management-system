package distributedSystem.Authorization.dto;

public record CreateOrGetUserReply(CreateOrGetUserStatus status, Long userId, String message) {}
