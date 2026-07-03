package kopo.integrix.service.impl;


/**
 * 텍스트 분석 비즈니스 로직입니다. LLM 분석 결과를 받아 위험 점수와 판단 근거를 만들고, 분석 실패 시 명확한 오류를 반환합니다.
 */
import kopo.integrix.dto.mongo.AnalysisDetailDTO;
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.dto.text.TextAnalysisResponseDTO;
import kopo.integrix.repository.mongo.AnalysisDetailRepository;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.service.ExternalAnalysisException;
import kopo.integrix.service.TextAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TextAnalysisServiceImpl implements TextAnalysisService {

    private static final String LABEL_SAFE = "safe";
    private static final String LABEL_CAUTION = "caution";
    private static final String LABEL_DANGEROUS = "dangerous";
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+|\\n+");

    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisDetailRepository analysisDetailRepository;
    private final TextLlmAnalysisClient textLlmAnalysisClient;

    @Override
    public TextAnalysisResponseDTO analyzeText(String text, String userId) {
        // 분석 전 공백을 정리해 같은 내용이 입력 형식 차이로 다르게 분석되지 않게 합니다.
        String sourceText = normalizeText(text);

        if (sourceText.length() < 10) {
            throw new IllegalArgumentException("텍스트는 10자 이상 입력해주세요.");
        }

        // LLM 설정이 있으면 LLM 결과를 우선 사용하고, 설정이 없을 때만 로컬 규칙 분석으로 대체합니다.
        AnalysisPipelineResult analysis = textLlmAnalysisClient.analyze(sourceText)
                .map(llmResult -> fromLlmResult(llmResult, sourceText))
                .orElseGet(() -> {
                    // LLM을 쓰기로 한 운영 환경에서 실패하면 0점 결과로 숨기지 않고 분석 실패로 응답합니다.
                    if (textLlmAnalysisClient.isConfigured()) {
                        throw new ExternalAnalysisException("텍스트 외부 분석 시간이 초과되었거나 실패했습니다. 잠시 후 다시 시도해주세요.");
                    }
                    return analyzeWithRules(sourceText);
                });

        // 화면에서는 신뢰도와 위험도를 함께 보여주므로 신뢰도 기준 결과를 위험 점수로 변환합니다.
        int riskScore = 100 - analysis.trustScore();
        TextAnalysisResponseDTO response = new TextAnalysisResponseDTO(
                sourceText,
                convertScoreToTrustLevel(riskScore),
                analysis.trustScore(),
                riskScore,
                new TextAnalysisResponseDTO.Analysis(
                        analysis.keyClaims().stream().limit(10).toList(),
                        analysis.riskyExpressions().stream().limit(8).toList(),
                        analysis.sentences(),
                        analysis.recommendations()
                )
        );
        // 분석 결과는 마이페이지 분석 기록에서 다시 볼 수 있도록 MongoDB에 저장합니다.
        saveAnalysis(sourceText, userId, analysis, response);
        return response;
    }

    private String normalizeText(String text) {
        String normalizedText = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalizedText.isBlank()) {
            throw new IllegalArgumentException("분석할 텍스트를 입력해주세요.");
        }
        return normalizedText;
    }

    private AnalysisPipelineResult fromLlmResult(TextLlmAnalysisClient.LlmAnalysisResult llmResult, String sourceText) {
        // 외부 LLM 응답값은 항상 0~100 범위와 내부 trustLevel 값으로 정규화합니다.
        int trustScore = clamp(llmResult.trustScore());
        String trustLevel = normalizeTrustLevel(llmResult.trustLevel(), trustScore);
        List<String> riskyExpressions = llmResult.riskyExpressions() == null ? List.of() : llmResult.riskyExpressions();
        Set<String> seenSentences = new LinkedHashSet<>();
        // LLM이 원문에 없는 문장을 만들어내는 경우를 막기 위해 원문에 포함된 문장만 사용합니다.
        List<TextAnalysisResponseDTO.SentenceAnalysis> sentences = llmResult.sentences().stream()
                .map(sentence -> new TextAnalysisResponseDTO.SentenceAnalysis(
                        safeText(sentence.text()),
                        normalizeSeverity(sentence.severity()),
                        safeText(sentence.reason())
                ))
                .filter(sentence -> !sentence.text().isBlank())
                .filter(sentence -> isFromSourceText(sentence.text(), sourceText))
                .filter(sentence -> seenSentences.add(normalizeForSentenceMatch(sentence.text())))
                .toList();

        // 위험 표현도 없고 모든 문장이 normal이면 결과를 안전 쪽으로 보정합니다.
        if (riskyExpressions.isEmpty()
                && sentences.stream().allMatch(sentence -> "normal".equals(sentence.severity()))) {
            trustScore = 100;
            trustLevel = "reliable";
        }

        return new AnalysisPipelineResult(
                trustLevel,
                trustScore,
                nonEmpty(llmResult.keyClaims(), representativeClaim(sourceText)),
                riskyExpressions,
                sentences.isEmpty()
                        ? List.of(new TextAnalysisResponseDTO.SentenceAnalysis(
                        representativeClaim(sourceText),
                        "normal",
                        "문장 단위로 뚜렷한 위험 표현이 감지되지 않았습니다."
                ))
                        : sentences,
                nonEmpty(llmResult.recommendations(), "중요한 정보라면 공식 출처나 원문과 함께 확인하세요.")
        );
    }

    private AnalysisPipelineResult analyzeWithRules(String sourceText) {
        // LLM 설정이 없을 때 사용하는 보조 분석으로, 키워드와 문장 패턴만으로 위험 신호를 계산합니다.
        List<String> sentences = splitSentences(sourceText);
        List<TextAnalysisResponseDTO.SentenceAnalysis> sentenceAnalyses = new ArrayList<>();
        Set<String> keyClaims = new LinkedHashSet<>();
        Set<String> riskyExpressions = new LinkedHashSet<>();
        Set<String> recommendations = new LinkedHashSet<>();

        int riskScore = 0;

        for (String sentence : sentences) {
            // 문장별로 자극적 표현, 단정 표현, 출처 불명 표현 같은 위험 신호를 검사합니다.
            RiskMatch riskMatch = inspectSentence(sentence);
            riskScore += riskMatch.score();

            if (riskMatch.score() > 0) {
                riskyExpressions.addAll(riskMatch.reasons());
            }

            if (looksLikeClaim(sentence)) {
                keyClaims.add(sentence);
            }

            sentenceAnalyses.add(new TextAnalysisResponseDTO.SentenceAnalysis(
                    sentence,
                    riskMatch.severity(),
                    String.join(", ", riskMatch.reasons())
            ));
        }

        // 문장 단위 검사에 더해 문서 전체의 출처 부족, 숫자 주장 여부를 추가로 반영합니다.
        riskScore += inspectDocument(sourceText, recommendations);
        riskScore = Math.min(100, riskScore);
        int trustScore = 100 - riskScore;

        if (keyClaims.isEmpty()) {
            keyClaims.add(sentences.get(0));
        }

        if (recommendations.isEmpty()) {
            recommendations.add("출처, 작성일, 다른 신뢰 가능한 자료를 함께 확인하세요.");
        }

        return new AnalysisPipelineResult(
                convertScoreToTrustLevel(riskScore),
                trustScore,
                keyClaims.stream().toList(),
                riskyExpressions.stream().toList(),
                sentenceAnalyses,
                recommendations.stream().toList()
        );
    }

    private void saveAnalysis(String sourceText,
                              String userId,
                              AnalysisPipelineResult analysis,
                              TextAnalysisResponseDTO response) {
        // 목록 화면용 요약 결과와 원본 응답 전체를 AnalysisResultDTO에 저장합니다.
        int riskScore = 100 - analysis.trustScore();
        String resultLabel = convertScoreToLabel(riskScore);
        Date now = new Date();

        AnalysisResultDTO result = analysisResultRepository.save(new AnalysisResultDTO(
                null,
                userId != null && !userId.isBlank() ? userId : "guest",
                "TEXT",
                abbreviate(sourceText, 300),
                riskScore,
                resultLabel,
                buildSummary(analysis.trustScore(), analysis.riskyExpressions()),
                now,
                response
        ));

        List<AnalysisDetailDTO> details = new ArrayList<>();
        // 위험 문장만 상세 기록에 저장해 마이페이지에서 근거를 확인할 수 있게 합니다.
        for (TextAnalysisResponseDTO.SentenceAnalysis sentenceAnalysis : analysis.sentences()) {
            if (!"normal".equals(sentenceAnalysis.severity())) {
                details.add(new AnalysisDetailDTO(
                        null,
                        result.resultId(),
                        sentenceAnalysis.text() + " - " + sentenceAnalysis.reason(),
                        convertSeverity(sentenceAnalysis.severity()),
                        now
                ));
            }
        }

        // 권장 사항은 위험도와 별개로 정보성 상세 항목으로 저장합니다.
        for (String recommendation : analysis.recommendations()) {
            details.add(new AnalysisDetailDTO(
                    null,
                    result.resultId(),
                    recommendation,
                    "info",
                    now
            ));
        }

        if (!details.isEmpty()) {
            analysisDetailRepository.saveAll(details);
        }
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String part : SENTENCE_SPLIT_PATTERN.split(text)) {
            String sentence = part.trim();
            if (!sentence.isBlank()) {
                sentences.add(sentence);
            }
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    private RiskMatch inspectSentence(String sentence) {
        // 한 문장에서 여러 위험 표현이 발견될 수 있으므로 점수와 사유를 누적합니다.
        String lower = sentence.toLowerCase(Locale.ROOT);
        List<String> reasons = new ArrayList<>();
        int score = 0;

        score += addIfContains(lower, reasons, 18, "감정적이거나 자극적인 표현",
                "shocking", "urgent", "breaking", "must see", "unbelievable",
                "충격", "긴급", "속보", "반드시 봐야", "믿기 힘든");
        score += addIfContains(lower, reasons, 20, "단정적이거나 검증하기 어려운 주장",
                "100%", "guaranteed", "always", "never", "cure",
                "무조건", "절대", "완치", "보장", "확실");
        score += addIfContains(lower, reasons, 15, "출처가 불명확한 표현",
                "sources say", "anonymous", "rumor", "it is known", "reportedly",
                "카더라", "익명", "소문", "알려졌다", "관계자");
        score += addIfContains(lower, reasons, 16, "클릭, 로그인, 결제 등을 유도하는 표현",
                "click", "verify", "login", "password", "account", "pay now",
                "클릭", "인증", "로그인", "비밀번호", "계정", "결제");
        score += addIfContains(lower, reasons, 12, "공유나 확산을 압박하는 표현",
                "share", "spread", "before deleted", "tell everyone",
                "공유", "확산", "삭제되기 전에", "널리 알려");

        if (sentence.chars().filter(ch -> ch == '!').count() >= 3) {
            score += 10;
            reasons.add("느낌표가 과도하게 사용되었습니다.");
        }

        if (sentence.length() > 180) {
            score += 5;
            reasons.add("한 문장에 많은 주장이 길게 담겨 있습니다.");
        }

        if (score >= 35) {
            return new RiskMatch(Math.min(score, 45), "danger", reasons);
        }
        if (score > 0) {
            return new RiskMatch(Math.min(score, 30), "warning", reasons);
        }
        return new RiskMatch(0, "normal", List.of("뚜렷한 위험 표현은 발견되지 않았습니다."));
    }

    private int inspectDocument(String text, Set<String> recommendations) {
        String lower = text.toLowerCase(Locale.ROOT);
        int score = 0;

        boolean hasSource = lower.contains("source")
                || lower.contains("official")
                || lower.contains("report")
                || lower.contains("http://")
                || lower.contains("https://");

        if (!hasSource) {
            recommendations.add("명확한 출처가 보이지 않습니다. 공식 자료나 원문 출처를 함께 확인하세요.");
        }

        if (text.length() < 80) {
            recommendations.add("텍스트가 짧아 결과는 참고 신호로 보는 것이 좋습니다.");
        }

        if (containsSpecificDateOrNumber(text) && !hasSource) {
            score += 15;
            recommendations.add("구체적인 날짜, 숫자, 기관명은 신뢰 가능한 출처와 대조해 확인하세요.");
        }

        return score;
    }

    private boolean containsSpecificDateOrNumber(String text) {
        return text.matches(".*\\d+.*");
    }

    private int addIfContains(String lowerSentence, List<String> reasons, int score, String reason, String... keywords) {
        for (String keyword : keywords) {
            if (lowerSentence.contains(keyword.toLowerCase(Locale.ROOT))) {
                reasons.add(reason + " (" + keyword + ")");
                return score;
            }
        }
        return 0;
    }

    private boolean looksLikeClaim(String sentence) {
        if (sentence.length() < 10) {
            return false;
        }

        if (sentence.matches(".*\\d+.*")) {
            return true;
        }

        String lower = sentence.toLowerCase(Locale.ROOT);
        return lower.contains("announced")
                || lower.contains("confirmed")
                || lower.contains("reported")
                || lower.contains("according to")
                || lower.contains("because")
                || lower.contains("therefore")
                || sentence.length() >= 25;
    }

    private String convertScoreToTrustLevel(int riskScore) {
        if (riskScore >= 70) {
            return "dangerous";
        }
        if (riskScore >= 35) {
            return "needs-verification";
        }
        return "reliable";
    }

    private String convertTrustLevelToLabel(String trustLevel) {
        return switch (trustLevel) {
            case "reliable" -> LABEL_SAFE;
            case "needs-verification" -> LABEL_CAUTION;
            case "dangerous" -> LABEL_DANGEROUS;
            default -> LABEL_CAUTION;
        };
    }

    private String convertScoreToLabel(int riskScore) {
        return convertTrustLevelToLabel(convertScoreToTrustLevel(riskScore));
    }

    private String normalizeTrustLevel(String trustLevel, int trustScore) {
        if ("reliable".equals(trustLevel) || "needs-verification".equals(trustLevel) || "dangerous".equals(trustLevel)) {
            return trustLevel;
        }
        if (trustScore >= 65) {
            return "reliable";
        }
        if (trustScore >= 30) {
            return "needs-verification";
        }
        return "dangerous";
    }

    private String normalizeSeverity(String severity) {
        return switch (severity) {
            case "normal", "warning", "danger" -> severity;
            default -> "warning";
        };
    }

    private String convertSeverity(String severity) {
        return switch (severity) {
            case "danger" -> "high";
            case "warning" -> "medium";
            default -> "low";
        };
    }

    private String buildSummary(int trustScore, List<String> riskyExpressions) {
        if (riskyExpressions == null || riskyExpressions.isEmpty()) {
            return "위험도 점수 " + (100 - trustScore) + "점. 뚜렷한 위험 표현은 발견되지 않았습니다.";
        }
        return "위험도 점수 " + (100 - trustScore) + "점. " + String.join(", ", riskyExpressions.stream().limit(3).toList());
    }

    private List<String> nonEmpty(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return List.of(fallback);
        }
        return values;
    }

    private String representativeClaim(String text) {
        List<String> sentences = splitSentences(text);
        String firstSentence = sentences.isEmpty() ? text : sentences.get(0);
        if (firstSentence.length() > 120) {
            return firstSentence.substring(0, 120) + "...";
        }
        return firstSentence;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isFromSourceText(String sentence, String sourceText) {
        String normalizedSentence = normalizeForSentenceMatch(sentence);
        String normalizedSource = normalizeForSentenceMatch(sourceText);
        return !normalizedSentence.isBlank() && normalizedSource.contains(normalizedSentence);
    }

    private String normalizeForSentenceMatch(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", "")
                .replaceAll("[\"'“”‘’]", "")
                .trim();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String abbreviate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private record RiskMatch(int score, String severity, List<String> reasons) {
    }

    private record AnalysisPipelineResult(
            String trustLevel,
            int trustScore,
            List<String> keyClaims,
            List<String> riskyExpressions,
            List<TextAnalysisResponseDTO.SentenceAnalysis> sentences,
            List<String> recommendations
    ) {
    }
}
