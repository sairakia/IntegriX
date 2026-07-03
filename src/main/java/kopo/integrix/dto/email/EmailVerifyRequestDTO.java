package kopo.integrix.dto.email;


/**
 * 이메일 인증번호 확인 요청 DTO입니다. 이메일과 사용자가 입력한 인증번호를 담아 검증에 사용합니다.
 */
public record EmailVerifyRequestDTO(
        String email,
        String code,
        String type
) {
}
