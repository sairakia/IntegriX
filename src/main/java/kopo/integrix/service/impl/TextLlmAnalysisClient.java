package kopo.integrix.service.impl;


/**
 * 텍스트 분석용 LLM 외부 API Client입니다. 프롬프트 구성, API 요청, 응답 파싱, LLM 설정 여부 확인을 담당합니다.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextLlmAnalysisClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.text.enabled:false}")
    private boolean enabled;

    @Value("${llm.text.api-url:}")
    private String apiUrl;

    @Value("${llm.text.api-key:}")
    private String apiKey;

    @Value("${llm.text.model:}")
    private String model;

    public boolean isConfigured() {
        return enabled && !isBlank(apiUrl) && !isBlank(apiKey) && !isBlank(model);
    }

    public Optional<LlmAnalysisResult> analyze(String text) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = buildResponsesRequest(text);

            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(25));

            if (isBlank(response)) {
                return Optional.empty();
            }

            String content = extractResponseText(objectMapper.readTree(response));
            if (isBlank(content)) {
                return Optional.empty();
            }

            JsonNode result = objectMapper.readTree(extractJson(content));
            return Optional.of(new LlmAnalysisResult(
                    clamp(result.path("trustScore").asInt(50)),
                    normalizeTrustLevel(result.path("trustLevel").asText("needs-verification")),
                    readStringList(result.path("keyClaims")),
                    readStringList(result.path("riskyExpressions")),
                    readSentences(result.path("sentences")),
                    readStringList(result.path("recommendations"))
            ));
        } catch (WebClientResponseException e) {
            log.warn("LLM text analysis failed with HTTP {}. Response body: {}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LLM text analysis failed.", e);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildResponsesRequest(String text) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "trustScore",
                "trustLevel",
                "keyClaims",
                "riskyExpressions",
                "sentences",
                "recommendations"
        ));
        schema.put("properties", Map.of(
                "trustScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                "trustLevel", Map.of("type", "string", "enum", List.of("reliable", "needs-verification", "dangerous")),
                "keyClaims", Map.of(
                        "type", "array",
                        "description", "사용자에게 표시할 한국어 핵심 주장 목록",
                        "items", Map.of("type", "string")
                ),
                "riskyExpressions", Map.of(
                        "type", "array",
                        "description", "사용자에게 표시할 한국어 신뢰도 판단 근거 목록",
                        "items", Map.of("type", "string")
                ),
                "sentences", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("text", "severity", "reason"),
                                "properties", Map.of(
                                        "text", Map.of("type", "string"),
                                        "severity", Map.of("type", "string", "enum", List.of("normal", "warning", "danger")),
                                        "reason", Map.of("type", "string", "description", "사용자에게 표시할 한국어 사유")
                                )
                        )
                ),
                "recommendations", Map.of(
                        "type", "array",
                        "description", "사용자에게 표시할 한국어 권장 확인 사항",
                        "items", Map.of("type", "string")
                )
        ));

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "text_credibility_analysis");
        format.put("strict", true);
        format.put("schema", schema);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", """
                너는 한국어 텍스트의 신뢰도와 사실성을 분석하는 평가자다.
                모든 사용자 표시용 응답은 반드시 자연스러운 한국어로 작성한다.
                영어 원문이 필요한 경우에도 괄호 안 번역을 제외하고는 한국어 설명을 우선한다.
                keyClaims, riskyExpressions, sentences.reason, recommendations 값에는 영어 문장을 쓰지 않는다.

                평가 기준:
                - 텍스트가 사실일 가능성이 있는지, 근거가 충분한지, 검증 가능한지 판단한다.
                - 근거 없는 단정, 모호한 출처, 과장된 주장, 검증 불가능한 사실 주장, 오해를 부르는 표현은 신뢰도를 낮춘다.
                - 짧은 텍스트나 출처 없음만으로 위험하다고 쓰지 말고, "검증 한계" 또는 "확인 필요"로 설명한다.
                - 강한 검증 근거나 출처 문맥이 없으면 100점을 주지 않는다.
                - 날짜, 숫자, 인명, 기관명, 인용문, 장소, 사건 주장은 텍스트 자체에 근거가 없으면 보통 검증 필요한 주장으로 본다.
                - 단, 널리 알려진 상식, 고정된 달력 사실, 일반적으로 인정되는 정의는 출처가 없어도 낮은 신뢰로 판단하지 않는다.
                - 예: "대부분의 국가에서 12월 25일은 크리스마스다"는 신뢰 가능에 가깝다.
                - 출처 없이 구체적인 날짜나 숫자가 있더라도, 그것이 공휴일/달력/정의처럼 안정적인 일반 지식이면 trustScore를 과도하게 낮추지 않는다.
                - 출처 없이 구체적인 날짜나 숫자가 있고 뉴스성 사건, 최근 발생 사건, 수치 통계, 특정 인물/기관의 발언이면 trustScore는 최대 85로 제한한다.
                - 핵심 주장이 날짜, 숫자, 고유명사에 의존하고 출처가 없으면 "needs-verification"을 사용한다.
                  단, 널리 알려진 달력 사실이나 정의는 예외다.
                - 가능하면 주요 주장을 5~10개 추출한다. 구체적 사실, 고유명사, 숫자, 날짜, 인과관계, 비교, 검증 필요한 주장을 포함한다.

                분석할 텍스트:
                """ + "\n\n" + text);
        body.put("text", Map.of("format", format));
        return body;
    }

    private String extractResponseText(JsonNode root) {
        String outputText = root.path("output_text").asText("");
        if (!isBlank(outputText)) {
            return outputText;
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                String text = part.path("text").asText("");
                if (!isBlank(text)) {
                    builder.append(text);
                }
            }
        }
        return builder.toString();
    }

    private List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
    }

    private List<LlmSentenceAnalysis> readSentences(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, LlmSentenceAnalysis.class)
        );
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String normalizeTrustLevel(String trustLevel) {
        return switch (trustLevel) {
            case "reliable", "needs-verification", "dangerous" -> trustLevel;
            default -> "needs-verification";
        };
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LlmAnalysisResult(
            int trustScore,
            String trustLevel,
            List<String> keyClaims,
            List<String> riskyExpressions,
            List<LlmSentenceAnalysis> sentences,
            List<String> recommendations
    ) {
    }

    public record LlmSentenceAnalysis(
            String text,
            String severity,
            String reason
    ) {
    }
}
