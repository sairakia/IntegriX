package kopo.integrix.dto;


/**
 * 신고/피드백 요청 DTO입니다. 신고 대상 URL, 신고 사유, 상세 내용을 Controller에서 Service로 전달합니다.
 */
public record ReportRequestDTO(
        String type,
        String content,
        String reason,
        String resultId
) {
}
