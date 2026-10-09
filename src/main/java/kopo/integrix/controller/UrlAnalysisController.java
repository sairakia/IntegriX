package kopo.integrix.controller;


/**
 * URL 분석 요청을 받는 Controller입니다. 프론트엔드의 /api/url/analyze 요청을 UrlAnalysisService로 전달하고 분석 결과를 응답합니다.
 */
import kopo.integrix.dto.url.UrlAnalysisRequestDTO;
import kopo.integrix.dto.url.UrlAnalysisResponseDTO;
import kopo.integrix.service.UrlAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/url")
public class UrlAnalysisController {

    private final UrlAnalysisService urlAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeUrl(@RequestBody UrlAnalysisRequestDTO dto,
                                        @AuthenticationPrincipal String userId) {
        try {
            UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl(dto.url(), userId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            log.error("URL analysis failed", e);
            return ResponseEntity.internalServerError().body("URL 분석 중 오류가 발생했습니다.");
        }
    }
}
