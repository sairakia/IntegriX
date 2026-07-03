package kopo.integrix.dto.dashboard;


/**
 * 대시보드 전체 응답 DTO입니다. 요약, 최근 분석, 월별 추이, 분포 데이터를 하나의 응답으로 묶어 전달합니다.
 */
import java.util.List;

public record DashboardResponseDTO(
        SummaryDTO summary,
        List<DistributionDTO> distribution,
        List<MonthlyTrendDTO> monthlyTrend,
        List<RecentAnalysisDTO> recentRecords
) {
}
