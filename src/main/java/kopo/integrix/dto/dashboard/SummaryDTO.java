package kopo.integrix.dto.dashboard;


/**
 * 대시보드 요약 DTO입니다. 전체 분석 수, 위험 항목 수 같은 상단 요약 지표를 전달합니다.
 */
public record SummaryDTO(
        long totalAnalysisCount,
        long dangerUrlCount,
        long dangerTextCount,
        long dangerImageCount
) {
}
