package kopo.integrix.service.impl;


/**
 * 분석 기록 조회 Service 구현체입니다. MongoDB에 저장된 분석 결과를 현재 사용자 기준으로 조회하고 화면용 DTO로 변환합니다.
 */
import kopo.integrix.dto.history.HistoryItemDTO;
import kopo.integrix.dto.mongo.AnalysisDetailDTO;
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.repository.mongo.AnalysisDetailRepository;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.service.AnalysisHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
public class AnalysisHistoryServiceImpl implements AnalysisHistoryService {

    private static final String LABEL_SAFE = "안전";
    private static final String LABEL_CAUTION = "주의";
    private static final String LABEL_DANGEROUS = "위험";
    private static final TimeZone KOREA_TIME_ZONE = TimeZone.getTimeZone("Asia/Seoul");

    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisDetailRepository analysisDetailRepository;

    @Override
    public List<HistoryItemDTO> getHistory(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        dateFormat.setTimeZone(KOREA_TIME_ZONE);
        return analysisResultRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(result -> toHistoryItem(result, analysisDetailRepository.findByResultId(result.resultId()), dateFormat))
                .toList();
    }

    private HistoryItemDTO toHistoryItem(AnalysisResultDTO result,
                                         List<AnalysisDetailDTO> details,
                                         SimpleDateFormat dateFormat) {
        String type = normalizeType(result.analysisType());
        String label = normalizeLabel(result.resultLabel());
        List<String> issues = details.stream()
                .map(AnalysisDetailDTO::issueContent)
                .filter(value -> value != null && !value.isBlank())
                .toList();

        return new HistoryItemDTO(
                result.resultId(),
                type,
                result.inputData() != null ? result.inputData() : "",
                label,
                result.createdAt() != null ? dateFormat.format(result.createdAt()) : "",
                result.score() != null ? result.score() : 0,
                buildDetails(type, result, details, issues),
                result.rawResult()
        );
    }

    private HistoryItemDTO.HistoryDetailsDTO buildDetails(String type,
                                                          AnalysisResultDTO result,
                                                          List<AnalysisDetailDTO> details,
                                                          List<String> issues) {
        if ("텍스트".equals(type)) {
            return new HistoryItemDTO.HistoryDetailsDTO(
                    null,
                    new HistoryItemDTO.TextAnalysisHistoryDTO(
                            result.inputData() != null ? result.inputData() : "",
                            buildKeyClaims(result),
                            buildRiskyExpressions(details, issues),
                            buildSentences(details)
                    ),
                    null
            );
        }

        if ("이미지".equals(type)) {
            return new HistoryItemDTO.HistoryDetailsDTO(
                    null,
                    null,
                    new HistoryItemDTO.ImageAnalysisHistoryDTO(
                            result.inputData() != null ? result.inputData() : "",
                            new HistoryItemDTO.ImageMetadataHistoryDTO(null, null, null),
                            issues
                    )
            );
        }

        return new HistoryItemDTO.HistoryDetailsDTO(
                new HistoryItemDTO.UrlAnalysisHistoryDTO(issues),
                null,
                null
        );
    }

    private List<String> buildKeyClaims(AnalysisResultDTO result) {
        List<String> values = new ArrayList<>();
        if (result.summary() != null && !result.summary().isBlank()) {
            values.add(result.summary());
        }
        if (values.isEmpty() && result.inputData() != null && !result.inputData().isBlank()) {
            values.add(result.inputData());
        }
        return values;
    }

    private List<String> buildRiskyExpressions(List<AnalysisDetailDTO> details, List<String> fallback) {
        List<String> risky = details.stream()
                .filter(detail -> !isInfoSeverity(detail.severity()))
                .map(AnalysisDetailDTO::issueContent)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return risky.isEmpty() ? fallback : risky;
    }

    private List<HistoryItemDTO.SentenceHistoryDTO> buildSentences(List<AnalysisDetailDTO> details) {
        List<HistoryItemDTO.SentenceHistoryDTO> sentences = details.stream()
                .map(detail -> new HistoryItemDTO.SentenceHistoryDTO(
                        sentenceText(detail.issueContent()),
                        normalizeSentenceSeverity(detail.severity())
                ))
                .toList();

        if (sentences.isEmpty()) {
            return List.of(new HistoryItemDTO.SentenceHistoryDTO("상세 문장이 없습니다.", "normal"));
        }
        return sentences;
    }

    private String sentenceText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int separatorIndex = value.indexOf(" - ");
        return separatorIndex > 0 ? value.substring(0, separatorIndex) : value;
    }

    private boolean isInfoSeverity(String severity) {
        return severity == null
                || severity.isBlank()
                || "info".equalsIgnoreCase(severity)
                || "정보".equals(severity);
    }

    private String normalizeSentenceSeverity(String severity) {
        if (severity == null) {
            return "normal";
        }
        return switch (severity.toLowerCase()) {
            case "high", "높음", "danger" -> "danger";
            case "warning", "중간", "주의" -> "warning";
            default -> "normal";
        };
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return LABEL_CAUTION;
        }

        return switch (label) {
            case "safe", "reliable", "authentic", LABEL_SAFE -> LABEL_SAFE;
            case "caution", "needs-verification", LABEL_CAUTION -> LABEL_CAUTION;
            case "dangerous", "manipulated", LABEL_DANGEROUS -> LABEL_DANGEROUS;
            default -> LABEL_CAUTION;
        };
    }

    private String normalizeType(String type) {
        return switch (type == null ? "" : type) {
            case "TEXT" -> "텍스트";
            case "IMAGE" -> "이미지";
            default -> "URL";
        };
    }
}
