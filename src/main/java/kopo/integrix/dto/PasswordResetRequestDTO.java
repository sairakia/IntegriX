package kopo.integrix.dto;


/**
 * 비밀번호 재설정 요청 DTO입니다. 이메일 인증 이후 새 비밀번호로 변경할 때 필요한 값을 담습니다.
 */
public record PasswordResetRequestDTO(
        String userId,
        String email,
        String password
) {
}
