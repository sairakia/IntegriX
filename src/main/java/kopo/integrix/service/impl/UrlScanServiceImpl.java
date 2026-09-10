package kopo.integrix.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.integrix.service.UrlScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlScanServiceImpl implements UrlScanService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${urlscan.api.key:}")
    private String apiKey;

    @Value("${urlscan.search-url:https://urlscan.io/api/v1/search/}")
    private String searchUrl;

    @Value("${urlscan.enabled:true}")
    private boolean enabled;

    @Override
    public UrlScanLookupResult lookupUrl(String url) {
        if (!enabled) {
            return UrlScanLookupResult.skipped("urlscan.io 조회가 비활성화되어 있습니다.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("urlscan.io API key is not configured. Skipping urlscan lookup.");
            return UrlScanLookupResult.skipped("urlscan.io API 키가 설정되어 있지 않습니다.");
        }

        String domain = extractHost(url);
        if (domain == null) {
            return UrlScanLookupResult.skipped("urlscan.io 검색 대상 도메인을 확인할 수 없습니다.");
        }

        JsonNode body;
        try {
            body = requestSearchWithFallbacks(url, domain);
        } catch (WebClientResponseException.BadRequest e) {
            log.warn("urlscan.io rejected search. url={}, domain={}, status={}", url, domain, e.getStatusCode());
            return UrlScanLookupResult.skipped("urlscan.io 검색 요청 형식이 올바르지 않습니다.");
        } catch (WebClientResponseException e) {
            log.warn("urlscan.io lookup returned HTTP {}. url={}", e.getStatusCode(), url, e);
            return UrlScanLookupResult.skipped("urlscan.io 조회가 실패했습니다. HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("urlscan.io lookup failed. url={}", url, e);
            return UrlScanLookupResult.skipped("urlscan.io 조회 중 오류가 발생했습니다.");
        }

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

    private JsonNode requestSearchWithFallbacks(String url, String domain) throws Exception {
        String[] queries = {
                "page.url:\"" + url + "\"",
                "task.url:\"" + url + "\"",
                "page.domain:" + domain,
                "domain:" + domain,
                domain
        };

        JsonNode lastBody = null;
        for (String query : queries) {
            JsonNode body = requestSearch(query);
            if (body != null && body.path("results").isArray() && !body.path("results").isEmpty()) {
                return body;
            }
            lastBody = body;
        }
        return lastBody;
    }

    private JsonNode requestSearch(String query) throws Exception {
        String requestUri = UriComponentsBuilder
                .fromUriString(searchUrl)
                .queryParam("q", query)
                .queryParam("size", 5)
                .queryParam("datasource", "scans")
                .build()
                .encode()
                .toUriString();

        String body = webClient.get()
                .uri(requestUri)
                .header("api-key", apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

        return body == null || body.isBlank() ? null : objectMapper.readTree(body);
    }

    private String extractHost(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
            return null;
        }
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
