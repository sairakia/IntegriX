package kopo.integrix.dto;


/**
 * 공통 응답 DTO입니다. 성공 여부, 메시지, 데이터 형식을 통일해서 프론트엔드가 일관되게 처리할 수 있게 합니다.
 */
public record CommonResponseDTO(
        boolean success,
        String message,
        Object data
) {
}