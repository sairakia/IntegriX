package kopo.integrix.dto.dashboard;


/**
 * 대시보드 월별 추이 DTO입니다. 월 단위 분석 건수나 위험도 변화를 차트로 표시하기 위한 데이터를 담습니다.
 */
public record MonthlyTrendDTO(
        String month,
        long safe,
        long caution,
        long danger
) {
}
