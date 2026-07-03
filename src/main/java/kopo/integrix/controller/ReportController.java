package kopo.integrix.controller;


/**
 * 신고와 피드백 요청을 받는 Controller입니다. 사용자가 신고한 URL과 사유를 저장하고 분석 점수에 반영될 수 있게 합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.ReportRequestDTO;
import kopo.integrix.entity.ReportFeedbackEntity;
import kopo.integrix.repository.ReportFeedbackRepository;
import kopo.integrix.util.UrlNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {

    private final ReportFeedbackRepository reportFeedbackRepository;
    @PostMapping
    public CommonResponseDTO createReport(@RequestBody ReportRequestDTO request,
                                          @AuthenticationPrincipal String userId) {
        if (request == null || isBlank(request.type()) || isBlank(request.content()) || isBlank(request.reason())) {
            return new CommonResponseDTO(false, "신고할 내용을 입력해주세요.", null);
        }

        String content;
        try {
            content = normalizeContent(request.type(), request.content());
        } catch (IllegalArgumentException e) {
            return new CommonResponseDTO(false, e.getMessage(), null);
        }

        reportFeedbackRepository.save(ReportFeedbackEntity.create(
                userId,
                request.resultId(),
                "URL",
                content,
                request.reason().trim()
        ));

        return new CommonResponseDTO(true, "신고 내용이 접수되었습니다.", null);
    }

    private String normalizeContent(String type, String content) {
        if (!"url".equalsIgnoreCase(type) && !"URL".equals(type)) {
            throw new IllegalArgumentException("URL만 신고할 수 있습니다.");
        }
        return UrlNormalizer.normalizeForReportMatch(content);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
