package kopo.integrix.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import kopo.integrix.service.UrlScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlScanServiceImpl implements UrlScanService {

    private final WebClient webClient;

    @Value("${urlscan.api.key:}")
    private String apiKey;

    @Value("${urlscan.search-url:https://urlscan.io/api/v1/search/}")
    private String searchUrl;

    @Value("${urlscan.enabled:true}")
    private boolean enabled;

    @Override
    public UrlScanLookupResult lookupUrl(String url) throws Exception {
        if (!enabled) {
            return UrlScanLookupResult.skipped("urlscan.io 조회가 비활성화되어 있습니다.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("urlscan.io API key is not configured. Skipping urlscan lookup.");
            return UrlScanLookupResult.skipped("urlscan.io API 키가 설정되어 있지 않습니다.");
        }

        String requestUri = UriComponentsBuilder
                .fromUriString(searchUrl)
                .queryParam("q", buildQuery(url))
                .queryParam("size", 5)
                .queryParam("datasource", "scans")
                .build()
                .encode()
                .toUriString();

        JsonNode body = webClient.get()
                .uri(requestUri)
                .header("api-key", apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));

        if (body == null || !body.path("results").isArray() || body.path("results").isEmpty()) {
            return UrlScanLookupResult.noResult();
        }

        int total = body.path("total").asInt(body.path("results").size());
        JsonNode selected = selectMostRelevantResult(body.path("results"));
        JsonNode verdicts = selected.path("verdicts");
        boolean malicious = verdicts.path("malicious").asBoolean(false)
                || verdicts.path("urlscan").path("malicious").asBoolean(false)
                || verdicts.path("engines").path("malicious").asBoolean(false)
                || verdicts.path("community").path("malicious").asBoolean(false);

        int score = readScore(verdicts);
        String scannedUrl = selected.path("page").path("url").asText(
                selected.path("task").path("url").asText(null)
        );
        String scannedAt = selected.path("task").path("time").asText(null);
        String resultUrl = selected.path("result").asText(null);

        return new UrlScanLookupResult(
                true,
                malicious,
                score,
                total,
                scannedUrl,
                scannedAt,
                resultUrl,
                malicious ? "기존 분석 기록에서 악성 verdict가 확인되었습니다." : "기존 분석 기록에서 악성으로 확인되지 않았습니다."
        );
    }

    private String buildQuery(String url) {
        String escapedUrl = url.replace("\\", "\\\\").replace("\"", "\\\"");
        return "page.url:\"" + escapedUrl + "\" OR task.url:\"" + escapedUrl + "\"";
    }

    private JsonNode selectMostRelevantResult(JsonNode results) {
        JsonNode first = results.get(0);
        for (JsonNode result : results) {
            JsonNode verdicts = result.path("verdicts");
            if (verdicts.path("malicious").asBoolean(false)
                    || verdicts.path("urlscan").path("malicious").asBoolean(false)
                    || verdicts.path("engines").path("malicious").asBoolean(false)
                    || verdicts.path("community").path("malicious").asBoolean(false)) {
                return result;
            }
        }
        return first;
    }

    private int readScore(JsonNode verdicts) {
        if (verdicts.has("score")) {
            return verdicts.path("score").asInt(0);
        }
        if (verdicts.path("urlscan").has("score")) {
            return verdicts.path("urlscan").path("score").asInt(0);
        }
        if (verdicts.path("engines").has("score")) {
            return verdicts.path("engines").path("score").asInt(0);
        }
        return 0;
    }
}
