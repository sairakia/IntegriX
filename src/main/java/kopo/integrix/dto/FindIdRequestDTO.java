package kopo.integrix.dto;


/**
 * 아이디 찾기 요청 DTO입니다. 이메일 등 사용자 확인에 필요한 값을 받아 계정 ID 조회에 사용합니다.
 */
public record FindIdRequestDTO(
        String email
) {
}
