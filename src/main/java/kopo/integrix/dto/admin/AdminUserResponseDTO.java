package kopo.integrix.dto.admin;

public record AdminUserResponseDTO(
        String userId,
        String email,
        String name,
        String role,
        String status,
        String profileImageUrl
) {
}
