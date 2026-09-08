package kopo.integrix.dto.admin;

public record UrlPolicyRequestDTO(
        String matchType,
        String pattern,
        String reason,
        Boolean active
) {
}
