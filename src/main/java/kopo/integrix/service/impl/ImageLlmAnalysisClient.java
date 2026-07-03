package kopo.integrix.service.impl;


/**
 * 이미지 분석용 LLM 외부 API Client입니다. 이미지 입력을 외부 LLM API로 보내고 분석 결과 JSON을 파싱합니다.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageLlmAnalysisClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.image.enabled:false}")
    private boolean enabled;

    @Value("${llm.image.api-url:}")
    private String apiUrl;

    @Value("${llm.image.api-key:}")
    private String apiKey;

    @Value("${llm.image.model:}")
    private String model;

    public boolean isConfigured() {
        return enabled && !isBlank(apiUrl) && !isBlank(apiKey) && !isBlank(model);
    }

    public Optional<LlmImageAnalysisResult> analyze(String inputName,
                                                    String contentType,
                                                    byte[] imageBytes,
                                                    String format,
                                                    int width,
                                                    int height,
                                                    boolean hasAlpha,
                                                    double complexity,
                                                    ImageAnalysisServiceImpl.AnalysisResult localAnalysis) {
        if (!isConfigured() || imageBytes == null || imageBytes.length == 0) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = buildResponsesRequest(
                    inputName,
                    contentType,
                    imageBytes,
                    format,
                    width,
                    height,
                    hasAlpha,
                    complexity,
                    localAnalysis
            );

            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(35));

            if (isBlank(response)) {
                return Optional.empty();
            }

            String content = extractResponseText(objectMapper.readTree(response));
            if (isBlank(content)) {
                return Optional.empty();
            }

            JsonNode result = objectMapper.readTree(extractJson(content));
            return Optional.of(new LlmImageAnalysisResult(
                    normalizeCredibility(result.path("credibility").asText("needs-verification")),
                    clamp(result.path("confidence").asInt(localAnalysis.confidence())),
                    readStringList(result.path("metadata")),
                    readStringList(result.path("manipulationIndicators")),
                    readStringList(result.path("recommendations"))
            ));
        } catch (WebClientResponseException e) {
            log.warn("LLM image analysis failed with HTTP {}. Response body: {}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LLM image analysis failed.", e);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildResponsesRequest(String inputName,
                                                      String contentType,
                                                      byte[] imageBytes,
                                                      String format,
                                                      int width,
                                                      int height,
                                                      boolean hasAlpha,
                                                      double complexity,
                                                      ImageAnalysisServiceImpl.AnalysisResult localAnalysis) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "credibility",
                "confidence",
                "metadata",
                "manipulationIndicators",
                "recommendations"
        ));
        schema.put("properties", Map.of(
                "credibility", Map.of("type", "string", "enum", List.of("authentic", "needs-verification", "manipulated")),
                "confidence", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                "metadata", Map.of(
                        "type", "array",
                        "items", Map.of("type", "string")
                ),
                "manipulationIndicators", Map.of(
                        "type", "array",
                        "items", Map.of("type", "string")
                ),
                "recommendations", Map.of(
                        "type", "array",
                        "items", Map.of("type", "string")
                )
        ));

        Map<String, Object> formatBody = new LinkedHashMap<>();
        formatBody.put("type", "json_schema");
        formatBody.put("name", "image_ai_composite_analysis");
        formatBody.put("strict", true);
        formatBody.put("schema", schema);

        String imageDataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        String prompt = """
                You analyze an image for AI-generated, synthetic, edited, or composited image signals.
                Write every response field in natural Korean.
                The service is for ordinary users, not developers.

                Important limits:
                - The goal is to tell users whether the image shows signs of AI generation, synthesis, editing, or compositing.
                - Do not claim the image is definitely real, definitely AI-generated, or definitely manipulated unless there is strong visible evidence.
                - Use "needs-verification" when AI/composite judgment is uncertain.
                - The confidence field means how confident you are that the image is authentic and not AI-generated/composited. Higher confidence means lower risk.
                - Treat screenshots, game UI, logos, posters, thumbnails, and simple graphics as hard to judge unless there are clear visual artifacts.
                - Look for practical visual signals: distorted text, unnatural hands/faces, inconsistent lighting, mismatched shadows, warped edges, repeated textures, pasted-looking objects, broken perspective, and inconsistent blur/noise.
                - Base your answer on visible content plus the provided internal server-side technical signals.
                - Keep each list item concise and user-facing.
                - Do not expose internal technical values such as pixel variation score, local confidence, server-side signal, alpha channel, or content type.
                - Do not list every visible UI number or stat unless it is essential for risk judgment.
                - Explain what the user should understand, for example "게임 화면 캡처로 보이며 AI 생성 여부보다는 화면 편집 여부 확인이 더 중요합니다."
                - The metadata array should contain 3 to 5 easy user-facing summary items only.
                - The manipulationIndicators array should explain AI-generation/composite caution signals in plain Korean wording like "주의 신호".
                - Do not put generic limitations such as small image size, low resolution, JPEG compression, noise, or "hard to verify" into manipulationIndicators unless there is a visible localized mismatch or concrete artifact.
                - Put those generic limitations in metadata or recommendations instead, and do not prefix them with "주의:".
                - If no strong AI/composite sign is visible, say that clearly and explain the limitation.
                - The recommendations array should be practical actions: compare with original source, check upload history, inspect original file, reverse image search, or verify in-app/original page when it is a screenshot.

                Internal server-side technical signals. Use these only for reasoning. Do not reveal them directly:
                - Input name: %s
                - Format: %s
                - Size: %d x %d px
                - Has alpha channel: %s
                - Pixel variation score: %.2f
                - Local confidence: %d
                - Local indicators: %s
                """.formatted(
                inputName,
                format,
                width,
                height,
                hasAlpha ? "yes" : "no",
                complexity,
                localAnalysis.confidence(),
                String.join(", ", localAnalysis.manipulationIndicators())
        );

        Map<String, Object> inputText = Map.of(
                "type", "input_text",
                "text", prompt
        );
        Map<String, Object> inputImage = Map.of(
                "type", "input_image",
                "image_url", imageDataUrl,
                "detail", "auto"
        );
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(inputText, inputImage)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(message));
        body.put("text", Map.of("format", formatBody));
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

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String normalizeCredibility(String credibility) {
        return switch (credibility) {
            case "authentic", "needs-verification", "manipulated" -> credibility;
            default -> "needs-verification";
        };
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LlmImageAnalysisResult(
            String credibility,
            int confidence,
            List<String> metadata,
            List<String> manipulationIndicators,
            List<String> recommendations
    ) {
    }
}
