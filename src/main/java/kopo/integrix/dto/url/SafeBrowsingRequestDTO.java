package kopo.integrix.dto.url;


/**
 * Google Safe Browsing API 요청 DTO입니다. 검사할 URL 목록과 threat type 정보를 외부 API 요청 JSON 형태로 구성합니다.
 */
import java.util.List;

public record SafeBrowsingRequestDTO(
        Client client,
        ThreatInfo threatInfo
) {
    public record Client(
            String clientId,
            String clientVersion
    ) {
    }

    public record ThreatInfo(
            List<String> threatTypes,
            List<String> platformTypes,
            List<String> threatEntryTypes,
            List<ThreatEntry> threatEntries
    ) {
    }

    public record ThreatEntry(
            String url
    ) {
    }
}