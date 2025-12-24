package distributedSystem.CustomerSupport.dto;

import jakarta.validation.constraints.NotBlank;



public record SendMessageRequest(
        @NotBlank String content
) {}
