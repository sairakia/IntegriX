package kopo.integrix.controller;


/**
 * 이미지 분석 요청을 받는 Controller입니다. 이미지 파일 또는 이미지 URL 요청을 ImageAnalysisService로 전달하고 분석 결과를 반환합니다.
 */
import kopo.integrix.dto.image.ImageAnalysisResponseDTO;
import kopo.integrix.service.ExternalAnalysisException;
import kopo.integrix.service.ImageAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/image")
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeImage(@RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                          @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                          @AuthenticationPrincipal String userId) {
        try {
            ImageAnalysisResponseDTO result = imageAnalysisService.analyzeImage(imageFile, imageUrl, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ExternalAnalysisException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (Exception e) {
            log.error("Image analysis failed", e);
            return ResponseEntity.internalServerError().body("이미지 분석 중 오류가 발생했습니다.");
        }
    }
}

