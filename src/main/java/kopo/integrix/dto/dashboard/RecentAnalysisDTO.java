package kopo.integrix.dto.dashboard;


/**
 * 대시보드 최근 분석 DTO입니다. 최근 분석 대상, 유형, 결과, 시간 정보를 화면에 표시하기 위한 응답 구조입니다.
 */
public record RecentAnalysisDTO(
        String type,
        String content,
        String status,
        String date
) {
}
