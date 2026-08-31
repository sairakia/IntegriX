package kopo.integrix.dto;


/**
 * 사용자 정보 응답 DTO입니다. Entity 전체를 노출하지 않고 화면에 필요한 사용자 정보만 반환합니다.
 */
public record UserResponseDTO(
        String id,
        String userId,
        String email,
        String name,
        String role,
        String profileImage
) {
}
