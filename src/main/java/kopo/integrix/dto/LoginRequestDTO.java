package kopo.integrix.dto;


/**
 * 로그인 요청 DTO입니다. 프론트엔드에서 보낸 userId와 password 값을 Controller로 전달합니다.
 */
public record LoginRequestDTO(
        String userId,
        String password
) {}