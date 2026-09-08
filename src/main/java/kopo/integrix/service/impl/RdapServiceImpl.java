package kopo.integrix.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.integrix.service.RdapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.IDN;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RdapServiceImpl implements RdapService {

    private static final String RDAP_DOMAIN_URL = "https://rdap.org/domain/";
    private static final int MAX_REDIRECTS = 3;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<RdapDomainInfo> lookupDomain(String host) {
        String normalizedHost = normalizeHost(host);
        if (normalizedHost == null) {
            return Optional.empty();
        }

        for (String candidate : buildDomainCandidates(normalizedHost)) {
            Optional<RdapDomainInfo> result = lookupCandidate(candidate);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    private Optional<RdapDomainInfo> lookupCandidate(String domain) {
        try {
            String responseBody = getRdapResponse(RDAP_DOMAIN_URL + domain, 0);

            if (responseBody == null || responseBody.isBlank()) {
                return Optional.empty();
            }

            JsonNode body = objectMapper.readTree(responseBody);
            String registrationDate = findEventDate(body, "registration");
            String expirationDate = findEventDate(body, "expiration");
            String registrar = findRegistrar(body);
            Long ageDays = calculateAgeDays(registrationDate);

            return Optional.of(new RdapDomainInfo(
                    body.path("ldhName").asText(domain).toLowerCase(),
                    registrar,
                    registrationDate,
                    expirationDate,
                    ageDays
            ));
        } catch (Exception e) {
            log.debug("RDAP lookup failed - domain: {}", domain, e);
            return Optional.empty();
        }
    }

    private String getRdapResponse(String url, int redirectCount) {
        ResponseEntity<String> response = webClient.get()
                .uri(url)
                .retrieve()
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

        if (response == null) {
            return null;
        }

        if (response.getStatusCode().is3xxRedirection()
                && response.getHeaders().getLocation() != null
                && redirectCount < MAX_REDIRECTS) {
            URI location = response.getHeaders().getLocation();
            return getRdapResponse(location.toString(), redirectCount + 1);
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            return null;
        }

        return response.getBody();
    }

    private String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }

        String normalized = host.trim().toLowerCase();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            return null;
        }

        return IDN.toASCII(normalized);
    }

    private String[] buildDomainCandidates(String host) {
        String[] labels = host.split("\\.");
        if (labels.length <= 2) {
            return new String[]{host};
        }

        String[] candidates = new String[labels.length - 1];
        for (int i = 0; i < labels.length - 1; i++) {
            candidates[i] = String.join(".", java.util.Arrays.copyOfRange(labels, i, labels.length));
        }
        return candidates;
    }

    private String findEventDate(JsonNode body, String eventAction) {
        JsonNode events = body.path("events");
        if (!events.isArray()) {
            return null;
        }

        for (JsonNode event : events) {
            if (eventAction.equalsIgnoreCase(event.path("eventAction").asText())) {
                return event.path("eventDate").asText(null);
            }
        }
        return null;
    }

    private String findRegistrar(JsonNode body) {
        JsonNode entities = body.path("entities");
        if (!entities.isArray()) {
            return null;
        }

        for (JsonNode entity : entities) {
            if (!hasRole(entity, "registrar")) {
                continue;
            }

            JsonNode vcardValues = entity.path("vcardArray").path(1);
            if (!vcardValues.isArray()) {
                continue;
            }

            for (JsonNode value : vcardValues) {
                if (value.isArray() && value.size() >= 4 && "fn".equalsIgnoreCase(value.get(0).asText())) {
                    return value.get(3).asText(null);
                }
            }
        }
        return null;
    }

    private boolean hasRole(JsonNode entity, String role) {
        JsonNode roles = entity.path("roles");
        if (!roles.isArray()) {
            return false;
        }

        for (JsonNode value : roles) {
            if (role.equalsIgnoreCase(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private Long calculateAgeDays(String registrationDate) {
        if (registrationDate == null || registrationDate.isBlank()) {
            return null;
        }

        try {
            return ChronoUnit.DAYS.between(Instant.parse(registrationDate), Instant.now());
        } catch (Exception e) {
            return null;
        }
    }
}
