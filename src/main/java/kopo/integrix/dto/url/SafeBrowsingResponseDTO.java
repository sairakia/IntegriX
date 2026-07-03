package kopo.integrix.dto.url;


/**
 * Google Safe Browsing API 응답을 매핑하는 DTO입니다. 외부 API JSON 중 위험 매칭 결과를 Java 객체로 변환합니다.
 */
import java.util.List;

public record SafeBrowsingResponseDTO(
        List<ThreatMatch> matches
) {
    public record ThreatMatch(
            String threatType,
            String platformType,
            String threatEntryType,
            ThreatEntry threat
    ) {
    }

    public record ThreatEntry(
            String url
    ) {
    }
}