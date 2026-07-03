package kopo.integrix.controller;


/**
 * 텍스트 분석 요청을 받는 Controller입니다. 사용자가 입력한 문장을 TextAnalysisService로 전달하고 위험도 분석 결과를 반환합니다.
 */
import kopo.integrix.dto.text.TextAnalysisRequestDTO;
import kopo.integrix.dto.text.TextAnalysisResponseDTO;
import kopo.integrix.service.ExternalAnalysisException;
import kopo.integrix.service.TextAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/text")
public class TextAnalysisController {

    private final TextAnalysisService textAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeText(@RequestBody TextAnalysisRequestDTO request,
                                         @AuthenticationPrincipal String userId) {
        try {
            TextAnalysisResponseDTO result = textAnalysisService.analyzeText(request.text(), userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ExternalAnalysisException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (Exception e) {
            log.error("Text analysis failed", e);
            return ResponseEntity.internalServerError().body("텍스트 분석 중 오류가 발생했습니다.");
        }
    }
}

