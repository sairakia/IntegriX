package kopo.integrix.controller;

import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.ReportFeedbackResponseDTO;
import kopo.integrix.dto.ReportRequestDTO;
import kopo.integrix.dto.ReportStatusUpdateRequestDTO;
import kopo.integrix.entity.ReportFeedbackEntity;
import kopo.integrix.repository.ReportFeedbackRepository;
import kopo.integrix.util.UrlNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {

    private static final Set<String> ALLOWED_STATUSES = Set.of("접수", "처리중", "완료", "반려");

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

    @GetMapping("/my")
    public CommonResponseDTO getMyReports(@AuthenticationPrincipal String userId) {
        if (isBlank(userId)) {
            return new CommonResponseDTO(false, "로그인이 필요합니다.", null);
        }

        List<ReportFeedbackResponseDTO> reports = reportFeedbackRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new CommonResponseDTO(true, "신고 접수 내역입니다.", reports);
    }

    @GetMapping("/admin")
    public CommonResponseDTO getAllReports() {
        List<ReportFeedbackResponseDTO> reports = reportFeedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
        return new CommonResponseDTO(true, "전체 신고 접수 내역입니다.", reports);
    }

    @PatchMapping("/admin/{feedbackId}")
    public CommonResponseDTO updateReportStatus(@PathVariable Long feedbackId,
                                                @RequestBody ReportStatusUpdateRequestDTO request) {
        if (request == null || isBlank(request.status()) || !ALLOWED_STATUSES.contains(request.status())) {
            return new CommonResponseDTO(false, "상태 값이 올바르지 않습니다.", null);
        }

        ReportFeedbackEntity report = reportFeedbackRepository.findById(feedbackId).orElse(null);
        if (report == null) {
            return new CommonResponseDTO(false, "신고 내역을 찾을 수 없습니다.", null);
        }

        report.updateStatus(request.status(), blankToNull(request.adminReply()));
        reportFeedbackRepository.save(report);
        return new CommonResponseDTO(true, "신고 처리 상태가 변경되었습니다.", toResponse(report));
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

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private ReportFeedbackResponseDTO toResponse(ReportFeedbackEntity report) {
        return new ReportFeedbackResponseDTO(
                report.getFeedbackId(),
                report.getUserId(),
                report.getResultId(),
                report.getFeedbackType(),
                report.getContent(),
                report.getReason(),
                report.getStatus(),
                report.getAdminReply(),
                report.getCreatedAt(),
                report.getProcessedAt(),
                report.getUpdatedAt()
        );
    }
}
