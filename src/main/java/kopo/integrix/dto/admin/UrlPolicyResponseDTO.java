package kopo.integrix.dto.admin;

import java.time.LocalDateTime;

public record UrlPolicyResponseDTO(
        Long policyId,
        String listType,
        String matchType,
        String pattern,
        String reason,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
