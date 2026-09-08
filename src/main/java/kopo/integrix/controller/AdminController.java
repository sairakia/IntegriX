package kopo.integrix.controller;

import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.admin.AdminSummaryResponseDTO;
import kopo.integrix.dto.admin.UrlPolicyRequestDTO;
import kopo.integrix.dto.admin.UrlPolicyResponseDTO;
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.entity.UrlPolicyEntity;
import kopo.integrix.repository.ReportFeedbackRepository;
import kopo.integrix.repository.UrlPolicyRepository;
import kopo.integrix.repository.UserInfoRepository;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.util.UrlNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private static final String USER_STATUS_ACTIVE = "ACTIVE";
    private static final String REPORT_STATUS_PENDING = "접수";
    private static final String REPORT_STATUS_IN_PROGRESS = "처리중";
    private static final Set<String> DANGER_LABELS = Set.of("위험", "dangerous", "manipulated");
    private static final String URL_POLICY_LIST_TYPE_BLOCK = "BLOCK";
    private static final String URL_POLICY_MATCH_TYPE_PATTERN = "PATTERN";

    private final UserInfoRepository userInfoRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ReportFeedbackRepository reportFeedbackRepository;
    private final UrlPolicyRepository urlPolicyRepository;

    @GetMapping("/summary")
    public CommonResponseDTO getAdminSummary() {
        long totalUsers = userInfoRepository.count();
        long activeUsers = userInfoRepository.countByStatus(USER_STATUS_ACTIVE);
        long totalReports = reportFeedbackRepository.count();
        long pendingReports = reportFeedbackRepository.countByStatus(REPORT_STATUS_PENDING);
        long inProgressReports = reportFeedbackRepository.countByStatus(REPORT_STATUS_IN_PROGRESS);

        AdminSummaryResponseDTO summary = new AdminSummaryResponseDTO(
                totalUsers,
                activeUsers,
                totalUsers - activeUsers,
                analysisResultRepository.count(),
                countDangerAnalysisResults(),
                totalReports,
                pendingReports,
                inProgressReports,
                totalReports - pendingReports - inProgressReports
        );

        return new CommonResponseDTO(true, "관리자 요약 통계입니다.", summary);
    }

    @GetMapping("/url-policies")
    public CommonResponseDTO getUrlPolicies() {
        List<UrlPolicyResponseDTO> policies = urlPolicyRepository.findAllByOrderByUpdatedAtDescPolicyIdDesc()
                .stream()
                .map(this::toUrlPolicyResponse)
                .toList();
        return new CommonResponseDTO(true, "URL 차단 목록입니다.", policies);
    }

    @PostMapping("/url-policies")
    public CommonResponseDTO createUrlPolicy(@RequestBody UrlPolicyRequestDTO request) {
        UrlPolicyInput input;
        try {
            input = validateUrlPolicyInput(request);
        } catch (IllegalArgumentException e) {
            return new CommonResponseDTO(false, e.getMessage(), null);
        }

        UrlPolicyEntity policy = urlPolicyRepository.save(UrlPolicyEntity.create(
                input.listType(),
                input.matchType(),
                input.pattern(),
                input.reason(),
                true
        ));
        return new CommonResponseDTO(true, "URL 목록 항목을 등록했습니다.", toUrlPolicyResponse(policy));
    }

    @PatchMapping("/url-policies/{policyId}")
    public CommonResponseDTO updateUrlPolicy(@PathVariable Long policyId,
                                             @RequestBody UrlPolicyRequestDTO request) {
        UrlPolicyEntity policy = urlPolicyRepository.findById(policyId).orElse(null);
        if (policy == null) {
            return new CommonResponseDTO(false, "URL 목록 항목을 찾을 수 없습니다.", null);
        }

        UrlPolicyInput input;
        try {
            input = validateUrlPolicyInput(request);
        } catch (IllegalArgumentException e) {
            return new CommonResponseDTO(false, e.getMessage(), null);
        }

        policy.update(input.listType(), input.matchType(), input.pattern(), input.reason(), true);
        urlPolicyRepository.save(policy);
        return new CommonResponseDTO(true, "URL 목록 항목을 수정했습니다.", toUrlPolicyResponse(policy));
    }

    @DeleteMapping("/url-policies/{policyId}")
    public CommonResponseDTO deleteUrlPolicy(@PathVariable Long policyId) {
        if (!urlPolicyRepository.existsById(policyId)) {
            return new CommonResponseDTO(false, "URL 목록 항목을 찾을 수 없습니다.", null);
        }

        urlPolicyRepository.deleteById(policyId);
        return new CommonResponseDTO(true, "URL 목록 항목을 삭제했습니다.", null);
    }

    private long countDangerAnalysisResults() {
        List<AnalysisResultDTO> results = analysisResultRepository.findAll();
        return results.stream()
                .filter(result -> result.resultLabel() != null)
                .filter(result -> DANGER_LABELS.contains(result.resultLabel()))
                .count();
    }

    private UrlPolicyInput validateUrlPolicyInput(UrlPolicyRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("등록할 URL 목록 정보를 입력해주세요.");
        }

        String pattern = request.pattern() == null ? "" : request.pattern().trim();
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("URL, 도메인 또는 키워드를 입력해주세요.");
        }

        String normalizedPattern = normalizePolicyPattern(pattern);

        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        return new UrlPolicyInput(URL_POLICY_LIST_TYPE_BLOCK, URL_POLICY_MATCH_TYPE_PATTERN, normalizedPattern, reason);
    }

    private String normalizePolicyPattern(String pattern) {
        String candidate = pattern.trim().toLowerCase(Locale.ROOT);
        boolean looksLikeUrlOrDomain = candidate.startsWith("http://")
                || candidate.startsWith("https://")
                || candidate.contains(".")
                || candidate.contains("/");

        if (looksLikeUrlOrDomain) {
            try {
                String normalizedUrl = UrlNormalizer.normalizeForReportMatch(candidate);
                URI uri = URI.create(normalizedUrl);
                if (uri.getHost() != null && !uri.getHost().isBlank()) {
                    return normalizedUrl;
                }
            } catch (Exception ignored) {
                // URL로 정규화되지 않는 값은 키워드/도메인 패턴으로 그대로 저장합니다.
            }
        }

        return candidate.replaceFirst("^\\.+", "").replaceFirst("\\.+$", "");
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private UrlPolicyResponseDTO toUrlPolicyResponse(UrlPolicyEntity policy) {
        return new UrlPolicyResponseDTO(
                policy.getPolicyId(),
                policy.getListType(),
                policy.getMatchType(),
                policy.getPattern(),
                policy.getReason(),
                policy.isActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    private record UrlPolicyInput(
            String listType,
            String matchType,
            String pattern,
            String reason
    ) {
    }
}
