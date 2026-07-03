package kopo.integrix.dto;


/**
 * 비밀번호 변경 요청 DTO입니다. 현재 비밀번호와 새 비밀번호를 받아 Service에서 검증과 변경을 처리할 수 있게 합니다.
 */
public record ChangePasswordRequestDTO(
        String currentPassword,
        String newPassword
) {
}
