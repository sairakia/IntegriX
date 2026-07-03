package kopo.integrix.controller;


/**
 * 대시보드 API Controller입니다. 분석 통계 요약 데이터를 DashboardService에서 받아 프론트엔드에 반환합니다.
 */
import kopo.integrix.dto.dashboard.DashboardResponseDTO;
import kopo.integrix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/dashboard", "/api/dashboard"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardResponseDTO> getDashboardSummary(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(dashboardService.getDashboard(userId));
    }
}
