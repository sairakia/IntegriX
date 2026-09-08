package kopo.integrix.dto.admin;

public record AdminSummaryResponseDTO(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long totalAnalysisCount,
        long dangerAnalysisCount,
        long totalReports,
        long pendingReports,
        long inProgressReports,
        long closedReports
) {
}
