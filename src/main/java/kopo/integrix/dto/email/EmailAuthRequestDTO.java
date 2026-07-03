package kopo.integrix.dto.email;


/**
 * 이메일 인증번호 발송 요청 DTO입니다. 인증번호를 받을 이메일 주소를 담습니다.
 */
public record EmailAuthRequestDTO(
        String email,
        String userId,
        String type
) {
}
