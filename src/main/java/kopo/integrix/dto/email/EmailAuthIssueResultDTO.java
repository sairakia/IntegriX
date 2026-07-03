package kopo.integrix.dto.email;


/**
 * 이메일 인증번호 발급 결과 DTO입니다. 인증번호 발급 성공 여부와 사용자 안내 메시지를 Service와 Controller 사이에서 전달합니다.
 */
public record EmailAuthIssueResultDTO(
        boolean success,
        String message,
        String email,
        String code,
        String type,
        String userId,
        long expireTime
) {
}
