package kopo.integrix.controller;


/**
 * 분석 기록 조회 Controller입니다. 로그인한 사용자의 분석 기록 목록 요청을 AnalysisHistoryService로 전달합니다.
 */
import kopo.integrix.dto.history.HistoryItemDTO;
import kopo.integrix.service.AnalysisHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/history")
public class AnalysisHistoryController {

    private final AnalysisHistoryService analysisHistoryService;

    @GetMapping
    public List<HistoryItemDTO> getHistory(@AuthenticationPrincipal String userId) {
        return analysisHistoryService.getHistory(userId);
    }
}
