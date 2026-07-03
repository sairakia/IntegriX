package kopo.integrix.dto;


/**
 * 회원탈퇴 요청 DTO입니다. 탈퇴 전 본인 확인에 필요한 비밀번호 등의 입력값을 담습니다.
 */
public record DeleteAccountRequestDTO(
        String password
) {
}
