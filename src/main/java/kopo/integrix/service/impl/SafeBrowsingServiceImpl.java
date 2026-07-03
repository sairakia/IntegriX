package kopo.integrix.service.impl;


/**
 * Google Safe Browsing API 호출을 담당하는 Service 구현체입니다. 외부 보안 DB에 URL 위험 여부를 조회하고 실패 시 서비스가 중단되지 않도록 처리합니다.
 */
import kopo.integrix.dto.url.SafeBrowsingRequestDTO;
import kopo.integrix.dto.url.SafeBrowsingResponseDTO;
import kopo.integrix.service.SafeBrowsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafeBrowsingServiceImpl implements SafeBrowsingService {

    private final WebClient webClient;

    @Value("${google.safe-browsing.api.key:}")
    private String apiKey;

    @Value("${google.safe-browsing.url}")
    private String safeBrowsingUrl;

    @Override
    public boolean isUnsafeUrl(String url) throws Exception {
        // API Key가 없는 개발 환경에서는 외부 조회를 건너뛰고 URL 분석 자체는 계속 진행합니다.
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Safe Browsing API key is not configured. Skipping Safe Browsing lookup.");
            return false;
        }

        // Google Safe Browsing API가 요구하는 threatInfo 구조에 검사할 URL을 담습니다.
        SafeBrowsingRequestDTO requestBody = new SafeBrowsingRequestDTO(
                new SafeBrowsingRequestDTO.Client("integrix", "1.0"),
                new SafeBrowsingRequestDTO.ThreatInfo(
                        List.of(
                                "MALWARE",
                                "SOCIAL_ENGINEERING",
                                "UNWANTED_SOFTWARE",
                                "POTENTIALLY_HARMFUL_APPLICATION"
                        ),
                        List.of("ANY_PLATFORM"),
                        List.of("URL"),
                        List.of(new SafeBrowsingRequestDTO.ThreatEntry(url))
                )
        );

        // 외부 API가 응답하지 않으면 분석 요청이 무한 대기하지 않도록 10초까지만 기다립니다.
        SafeBrowsingResponseDTO body =
                webClient.post()
                        .uri(safeBrowsingUrl + "?key=" + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(SafeBrowsingResponseDTO.class)
                        .block(Duration.ofSeconds(10));

        // matches가 하나라도 있으면 Google 보안 DB에 위험 URL로 등록된 것으로 판단합니다.
        return body != null
                && body.matches() != null
                && !body.matches().isEmpty();
    }
}
