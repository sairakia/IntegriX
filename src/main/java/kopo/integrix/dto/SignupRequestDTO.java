package kopo.integrix.dto;


/**
 * 회원가입 요청 DTO입니다. 사용자 ID, 이메일, 비밀번호 등 회원 생성에 필요한 입력값을 담습니다.
 */
public record SignupRequestDTO(
        String email,
        String password,
        String name,
        String userId
) {
}