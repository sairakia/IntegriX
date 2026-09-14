package kopo.integrix.service.impl;


/**
 * URL 위험도 분석의 핵심 비즈니스 로직입니다. URL 정규화, HTTPS/SSL 검사, 위험 패턴 검사, 신고 DB와 Safe Browsing 결과 반영, 점수 계산과 분석 기록 저장을 처리합니다.
 */
import kopo.integrix.dto.mongo.AnalysisDetailDTO;
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.dto.url.UrlAnalysisResponseDTO;
import kopo.integrix.repository.ReportFeedbackRepository;
import kopo.integrix.repository.mongo.AnalysisDetailRepository;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.service.RdapService;
import kopo.integrix.service.SafeBrowsingService;
import kopo.integrix.service.UrlAnalysisService;
import kopo.integrix.util.UrlNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlAnalysisServiceImpl implements UrlAnalysisService {

    private static final String LABEL_SAFE = "안전";
    private static final String LABEL_CAUTION = "주의";
    private static final String LABEL_DANGEROUS = "위험";

    private static final int SCORE_DNS_UNRESOLVED = 15;
    private static final int SCORE_SSL_CERTIFICATE_ERROR = 30;
    private static final int SCORE_HTTPS_CONNECTION_FAILED = 10;
    private static final int SCORE_LONG_URL = 8;
    private static final int SCORE_USER_INFO_MARKER = 20;
    private static final int SCORE_EXCESSIVE_HYPHENS = 8;
    private static final int SCORE_IP_ADDRESS_URL = 20;
    private static final int SCORE_MANY_SENSITIVE_KEYWORDS = 10;
    private static final int SCORE_SOME_SENSITIVE_KEYWORDS = 5;
    private static final int SCORE_SHORT_URL = 10;
    private static final int SCORE_REPORTED_URL = 25;
    private static final int SCORE_SAFE_BROWSING_THREAT = 50;
    private static final int SCORE_DOMAIN_AGE_UNDER_30_DAYS = 10;
    private static final int SCORE_DOMAIN_AGE_UNDER_90_DAYS = 5;
    private static final int MIN_CAUTION_SCORE = 15;
    private static final int MIN_SAFE_BROWSING_THREAT_SCORE = 70;

    private final RdapService rdapService;
    private final SafeBrowsingService safeBrowsingService;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisDetailRepository analysisDetailRepository;
    private final ReportFeedbackRepository reportFeedbackRepository;

    @Override
    public UrlAnalysisResponseDTO analyzeUrl(String url, String userId) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL을 입력해주세요.");
        }

        // Controller에서 넘어온 URL을 서비스에서 실제 분석 가능한 표준 URL로 정리합니다.
        String normalizedUrl = normalizeUrl(url.trim());
        URI uri = validatePublicUrl(normalizedUrl);
        String host = uri.getHost();
        boolean hostResolved = isHostResolvable(host);

        int riskScore = 0;

        List<String> httpsConnection = new ArrayList<>();
        List<String> sslCertificate = new ArrayList<>();
        List<String> domainAge = new ArrayList<>();
        List<String> blacklistStatus = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<UrlAnalysisResponseDTO.ScoreFactor> scoreFactors = new ArrayList<>();

        // URL 서비스 내부에서 DNS 확인 후 실제 HTTPS/SSL 연결 검사로 흐름이 이어집니다.
        HttpsConnectionStatus httpsConnectionStatus = hostResolved
                ? checkRealHttpsConnection(host)
                : HttpsConnectionStatus.CONNECTION_FAILED;

        if (!hostResolved) {
            httpsConnection.add("HTTPS 연결 검사: 도메인을 찾을 수 없어 연결을 확인하지 못했습니다.");
            sslCertificate.add("SSL 인증서 검사: 도메인을 찾을 수 없어 인증서를 확인하지 못했습니다.");
            recommendations.add("DNS 검사: 도메인 주소를 확인할 수 없습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "DNS 조회 실패", SCORE_DNS_UNRESOLVED, "도메인을 찾을 수 없어 실제 연결 상태를 확인하지 못했습니다.");
        } else if (httpsConnectionStatus == HttpsConnectionStatus.SECURE) {
            httpsConnection.add("HTTPS 연결 검사: 보안 연결이 정상입니다.");
            sslCertificate.add("SSL 인증서 검사: 인증서가 유효합니다.");
            recommendations.add("DNS 검사: 도메인 주소를 확인했습니다.");
        } else if (httpsConnectionStatus == HttpsConnectionStatus.SSL_CERTIFICATE_ERROR) {
            httpsConnection.add("HTTPS 연결 검사: SSL 인증서 문제로 연결을 신뢰할 수 없습니다.");
            sslCertificate.add("SSL 인증서 검사: 인증서 오류가 있습니다.");
            recommendations.add("SSL 인증서 검사: 인증서 신뢰 문제가 확인되었습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "SSL 인증서 오류", SCORE_SSL_CERTIFICATE_ERROR, "HTTPS 인증서 검증 중 오류가 확인되었습니다.");
        } else {
            httpsConnection.add("HTTPS 연결 검사: 보안 연결에 실패했습니다.");
            sslCertificate.add("SSL 인증서 검사: 연결 실패로 인증서를 확인하지 못했습니다.");
            recommendations.add("HTTPS 연결 검사: 보안 연결을 확인하지 못했습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "HTTPS 연결 실패", SCORE_HTTPS_CONNECTION_FAILED, "보안 연결을 완료하지 못해 신뢰도를 낮췄습니다.");
        }

        if (normalizedUrl.length() > 100) {
            recommendations.add("URL 길이 검사: URL이 비정상적으로 깁니다.");
            riskScore = addRisk(scoreFactors, riskScore, "긴 URL", SCORE_LONG_URL, "URL 길이가 100자를 초과합니다.");
        } else {
            recommendations.add("URL 길이 검사: URL 길이가 정상 범위입니다.");
        }

        if (isIpAddress(host)) {
            domainAge.add("도메인 등록 정보 확인: IP 주소 URL은 RDAP 도메인 조회 대상이 아닙니다.");
        } else {
            rdapService.lookupDomain(host).ifPresentOrElse(domainInfo -> {
                domainAge.add("도메인 등록 정보 확인: " + domainInfo.domain());

                if (domainInfo.registrationDate() != null) {
                    domainAge.add("도메인 등록일: " + domainInfo.registrationDate());
                }
                if (domainInfo.expirationDate() != null) {
                    domainAge.add("도메인 만료일: " + domainInfo.expirationDate());
                }
                if (domainInfo.registrar() != null && !domainInfo.registrar().isBlank()) {
                    domainAge.add("등록기관: " + domainInfo.registrar());
                }
                if (domainInfo.domainAgeDays() != null) {
                    domainAge.add("도메인 나이: " + domainInfo.domainAgeDays() + "일");
                }
            }, () -> domainAge.add("도메인 등록 정보 확인: RDAP 조회 결과를 가져오지 못했습니다."));
        }

        Long domainAgeDays = extractDomainAgeDays(domainAge);
        if (domainAgeDays != null && domainAgeDays < 30) {
            recommendations.add("도메인 나이 확인: 생성된 지 30일 미만인 최근 등록 도메인입니다.");
            riskScore = addRisk(scoreFactors, riskScore, "최근 등록 도메인", SCORE_DOMAIN_AGE_UNDER_30_DAYS, "RDAP 기준 도메인 생성 후 30일이 지나지 않았습니다.");
        } else if (domainAgeDays != null && domainAgeDays < 90) {
            recommendations.add("도메인 나이 확인: 생성된 지 90일 미만인 비교적 최근 등록 도메인입니다.");
            riskScore = addRisk(scoreFactors, riskScore, "최근 등록 도메인", SCORE_DOMAIN_AGE_UNDER_90_DAYS, "RDAP 기준 도메인 생성 후 90일이 지나지 않았습니다.");
        } else if (domainAgeDays != null) {
            recommendations.add("도메인 나이 확인: 최근 생성 도메인은 아닙니다.");
        }

        if (normalizedUrl.contains("@")) {
            recommendations.add("@ 문자 검사: @ 문자가 포함되어 피싱에 악용될 수 있습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "@ 문자 포함", SCORE_USER_INFO_MARKER, "URL에 사용자 정보 구분자인 @ 문자가 포함되어 피싱에 악용될 수 있습니다.");
        } else {
            recommendations.add("@ 문자 검사: @ 문자가 없습니다.");
        }

        long hyphenCount = normalizedUrl.chars().filter(ch -> ch == '-').count();
        if (hyphenCount >= 3) {
            recommendations.add("하이픈 검사: 하이픈 사용이 많습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "하이픈 과다 사용", SCORE_EXCESSIVE_HYPHENS, "URL에 하이픈이 3개 이상 포함되어 있습니다.");
        } else {
            recommendations.add("하이픈 검사: 하이픈 사용이 과도하지 않습니다.");
        }

        if (isIpAddress(host)) {
            recommendations.add("주소 형식 검사: IP 주소를 직접 사용한 URL입니다.");
            riskScore = addRisk(scoreFactors, riskScore, "IP 주소 직접 사용", SCORE_IP_ADDRESS_URL, "도메인 대신 IP 주소를 직접 사용했습니다.");
        } else {
            recommendations.add("주소 형식 검사: 일반 도메인 주소입니다.");
        }

        String lowerUrl = normalizedUrl.toLowerCase(Locale.ROOT);
        String[] keywords = {
                "login", "verify", "account", "secure",
                "update", "password", "signin", "free", "bonus"
        };

        int keywordCount = 0;
        for (String keyword : keywords) {
            if (lowerUrl.contains(keyword)) {
                keywordCount++;
            }
        }

        if (keywordCount >= 3) {
            recommendations.add("주의 키워드 검사: 로그인, 인증, 보안 관련 키워드가 여러 개 포함되어 있습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "주의 키워드 다수 포함", SCORE_MANY_SENSITIVE_KEYWORDS, "로그인, 인증, 보안 관련 키워드가 3개 이상 포함되어 있습니다.");
        } else if (keywordCount > 0) {
            recommendations.add("주의 키워드 검사: 로그인, 인증, 보안 관련 키워드가 일부 포함되어 있습니다.");
            riskScore = addRisk(scoreFactors, riskScore, "주의 키워드 포함", SCORE_SOME_SENSITIVE_KEYWORDS, "로그인, 인증, 보안 관련 키워드가 일부 포함되어 있습니다.");
        } else {
            recommendations.add("주의 키워드 검사: 주의 키워드가 없습니다.");
        }

        boolean shortUrl = false;
        String[] shortDomains = {
                "bit.ly", "tinyurl.com", "goo.gl",
                "t.co", "is.gd", "cutt.ly"
        };

        for (String shortDomain : shortDomains) {
            if (host.equalsIgnoreCase(shortDomain)) {
                shortUrl = true;
                break;
            }
        }

        if (shortUrl) {
            recommendations.add("단축 URL 검사: 단축 URL 서비스 주소입니다.");
            riskScore = addRisk(scoreFactors, riskScore, "단축 URL", SCORE_SHORT_URL, "단축 URL 서비스 주소라 실제 목적지를 바로 확인하기 어렵습니다.");
        } else {
            recommendations.add("단축 URL 검사: 단축 URL이 아닙니다.");
        }

        // URL 서비스에서 신고 피드백 DB로 이동해 기존 신고 URL과 일치하는지 조회합니다.
        boolean reportedUrl = reportFeedbackRepository.existsByFeedbackTypeAndContent(
                "URL",
                UrlNormalizer.normalizeForReportMatch(normalizedUrl)
        );
        if (reportedUrl) {
            blacklistStatus.add("신고 데이터베이스 검사: 신고된 URL입니다.");
            recommendations.add("신고 데이터베이스 검사: 신고된 URL입니다.");
            riskScore = addRisk(scoreFactors, riskScore, "신고된 URL", SCORE_REPORTED_URL, "사용자 신고 데이터베이스에 등록된 URL입니다.");
        } else {
            blacklistStatus.add("신고 데이터베이스 검사: 신고된 데이터가 없습니다.");
            recommendations.add("신고 데이터베이스 검사: 신고된 데이터가 없습니다.");
        }

        try {
            // URL 서비스에서 SafeBrowsingService로 이동해 Google Safe Browsing API 결과를 받아옵니다.
            boolean unsafe = safeBrowsingService.isUnsafeUrl(normalizedUrl);

            if (unsafe) {
                int beforeSafeBrowsingScore = riskScore;
                int afterSafeBrowsingScore = Math.max(
                        riskScore + SCORE_SAFE_BROWSING_THREAT,
                        MIN_SAFE_BROWSING_THREAT_SCORE
                );
                int addedScore = afterSafeBrowsingScore - beforeSafeBrowsingScore;

                blacklistStatus.add("Google Safe Browsing 검사: 위험 URL로 등록되어 있습니다.");
                recommendations.add("외부 보안 DB 위험 반영: Google Safe Browsing에서 위험 URL로 확인되어 "
                        + addedScore + "점을 추가했습니다.");
                if (addedScore > 0) {
                    scoreFactors.add(new UrlAnalysisResponseDTO.ScoreFactor(
                            "Google Safe Browsing 위험 감지",
                            addedScore,
                            "외부 보안 DB에서 위험 URL로 확인되어 최소 위험 점수 70점을 보장하도록 반영했습니다."
                    ));
                }
                riskScore = afterSafeBrowsingScore;
            } else {
                blacklistStatus.add("Google Safe Browsing 검사: 위험 URL로 등록되어 있지 않습니다.");
                recommendations.add("Google Safe Browsing 검사: 위험 URL로 등록되어 있지 않습니다.");
            }

        } catch (Exception e) {
            log.error("Safe Browsing lookup failed", e);
            blacklistStatus.add("Google Safe Browsing 검사: 외부 보안 DB 조회에 실패했습니다.");
            recommendations.add("Google Safe Browsing 검사: 외부 보안 DB 조회에 실패했습니다.");
        }

        if (riskScore > 100) {
            riskScore = 100;
        }

        String trustLevel;
        if (riskScore >= 70) {
            trustLevel = "dangerous";
        } else if (riskScore >= MIN_CAUTION_SCORE) {
            trustLevel = "caution";
        } else {
            trustLevel = "safe";
        }

        String resultLabel = convertTrustLevelToKorean(trustLevel);
        Date now = new Date();

        UrlAnalysisResponseDTO response = new UrlAnalysisResponseDTO(
                normalizedUrl,
                trustLevel,
                riskScore,
                scoreFactors,
                new UrlAnalysisResponseDTO.Analysis(
                        httpsConnection,
                        sslCertificate,
                        domainAge,
                        blacklistStatus,
                        recommendations
                )
        );

        // 분석 결과는 MongoDB 결과 컬렉션으로 저장되고, 상세 항목은 별도 상세 컬렉션으로 저장됩니다.
        AnalysisResultDTO result = analysisResultRepository.save(new AnalysisResultDTO(
                null,
                userId != null && !userId.isBlank() ? userId : "guest",
                "URL",
                normalizedUrl,
                riskScore,
                resultLabel,
                buildSummary(recommendations, blacklistStatus),
                now,
                response
        ));

        List<AnalysisDetailDTO> detailList = new ArrayList<>();
        addDetails(detailList, result.resultId(), httpsConnection, getSeverity(resultLabel), now);
        addDetails(detailList, result.resultId(), sslCertificate, getSeverity(resultLabel), now);
        addDetails(detailList, result.resultId(), domainAge, "정보", now);
        addDetails(detailList, result.resultId(), blacklistStatus, getSeverity(resultLabel), now);
        addDetails(detailList, result.resultId(), recommendations, getSeverity(resultLabel), now);

        if (!detailList.isEmpty()) {
            analysisDetailRepository.saveAll(detailList);
        }

        return response;
    }

    private int addRisk(List<UrlAnalysisResponseDTO.ScoreFactor> scoreFactors,
                        int currentScore,
                        String label,
                        int score,
                        String reason) {
        scoreFactors.add(new UrlAnalysisResponseDTO.ScoreFactor(label, score, reason));
        return currentScore + score;
    }

    private void addDetails(List<AnalysisDetailDTO> detailList,
                            String resultId,
                            List<String> items,
                            String severity,
                            Date createdAt) {
        for (String item : items) {
            detailList.add(new AnalysisDetailDTO(
                    null,
                    resultId,
                    item,
                    severity,
                    createdAt
            ));
        }
    }

    private Long extractDomainAgeDays(List<String> domainAge) {
        for (String item : domainAge) {
            if (item != null && item.startsWith("도메인 나이: ") && item.endsWith("일")) {
                try {
                    return Long.parseLong(item.substring("도메인 나이: ".length(), item.length() - 1));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String normalizeUrl(String url) {
        return UrlNormalizer.normalizeForAnalysis(url);
    }

    private URI validatePublicUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("올바른 URL 형식이 아닙니다.");
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("http 또는 https URL만 분석할 수 있습니다.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("도메인을 확인할 수 없습니다.");
        }

        if (!isIpAddress(host) && !isValidPublicHostnameSyntax(host)) {
            throw new IllegalArgumentException("분석 가능한 도메인 형식이 아닙니다. 예: example.com");
        }

        validatePublicHost(host);
        return uri;
    }

    private boolean isValidPublicHostnameSyntax(String host) {
        String normalizedHost = host.endsWith(".")
                ? host.substring(0, host.length() - 1)
                : host;

        if (normalizedHost.length() > 253 || !normalizedHost.contains(".")) {
            return false;
        }

        String[] labels = normalizedHost.split("\\.");
        if (labels.length < 2) {
            return false;
        }

        for (String label : labels) {
            if (label.isBlank()
                    || label.length() > 63
                    || label.startsWith("-")
                    || label.endsWith("-")
                    || !label.matches("[A-Za-z0-9-]+")) {
                return false;
            }
        }

        String topLevelDomain = labels[labels.length - 1];
        return topLevelDomain.matches("[A-Za-z]{2,}|xn--[A-Za-z0-9-]{2,}");
    }

    private void validatePublicHost(String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost) || lowerHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("localhost URL은 분석할 수 없습니다.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new IllegalArgumentException("내부망 또는 비공개 IP는 분석할 수 없습니다.");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Domain could not be resolved during public host validation - host: {}", host, e);
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private boolean isHostResolvable(String host) {
        try {
            InetAddress.getAllByName(host);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpsConnectionStatus checkRealHttpsConnection(String host) {
        HttpsURLConnection connection = null;

        try {
            validatePublicHost(host);

            URL httpsUrl = new URL("https://" + host);
            connection = (HttpsURLConnection) httpsUrl.openConnection();

            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);

            connection.connect();
            connection.getServerCertificates();

            return HttpsConnectionStatus.SECURE;

        } catch (SSLException e) {
            log.warn("SSL certificate check failed - host: {}", host, e);
            return HttpsConnectionStatus.SSL_CERTIFICATE_ERROR;
        } catch (Exception e) {
            log.warn("Real HTTPS connection check failed - host: {}", host, e);
            return HttpsConnectionStatus.CONNECTION_FAILED;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isIpAddress(String host) {
        try {
            InetAddress.getByName(host);
            return host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
        } catch (Exception e) {
            return false;
        }
    }

    private String convertTrustLevelToKorean(String trustLevel) {
        return switch (trustLevel) {
            case "safe" -> LABEL_SAFE;
            case "caution" -> LABEL_CAUTION;
            case "dangerous" -> LABEL_DANGEROUS;
            default -> LABEL_CAUTION;
        };
    }

    private String buildSummary(List<String> recommendations, List<String> blacklistStatus) {
        List<String> summaryParts = new ArrayList<>();

        if (!blacklistStatus.isEmpty()) {
            summaryParts.add(blacklistStatus.get(0));
        }

        if (!recommendations.isEmpty()) {
            summaryParts.add(recommendations.get(0));
        }

        return String.join(" / ", summaryParts);
    }

    private String getSeverity(String resultLabel) {
        return switch (resultLabel) {
            case LABEL_DANGEROUS -> "높음";
            case LABEL_CAUTION -> "중간";
            case LABEL_SAFE -> "낮음";
            default -> "중간";
        };
    }

    private enum HttpsConnectionStatus {
        SECURE,
        SSL_CERTIFICATE_ERROR,
        CONNECTION_FAILED
    }
}
