package kopo.integrix.util;


/**
 * URL 문자열을 분석하기 전에 표준 형태로 정리하는 유틸 클래스입니다. 프로토콜 누락, 공백, 대소문자 같은 입력 차이를 줄여 분석 로직이 일관되게 동작하도록 합니다.
 */
import java.net.URI;
import java.util.Locale;

public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    public static String normalizeForAnalysis(String url) {
        String normalizedUrl = ensureScheme(removeDuplicatedHttpScheme(url));
        URI uri = URI.create(normalizedUrl);

        String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
        return normalize(uri, scheme);
    }

    public static String normalizeForReportMatch(String url) {
        URI uri = URI.create(normalizeForAnalysis(url));
        return normalize(uri, "https");
    }

    private static String normalize(URI uri, String scheme) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        int port = normalizePort(scheme, uri.getPort());
        String path = normalizePath(uri.getRawPath());

        try {
            return new URI(
                    scheme,
                    uri.getRawUserInfo(),
                    host,
                    port,
                    path,
                    uri.getRawQuery(),
                    null
            ).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("올바른 URL 형식이 아닙니다.");
        }
    }

    private static String removeDuplicatedHttpScheme(String url) {
        String normalizedUrl = url == null ? "" : url.trim();
        while (hasDuplicatedHttpScheme(normalizedUrl)) {
            normalizedUrl = normalizedUrl.substring(normalizedUrl.indexOf("://") + 3);
        }
        return normalizedUrl;
    }

    private static String ensureScheme(String url) {
        if (url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            return url;
        }
        return "https://" + url;
    }

    private static boolean hasDuplicatedHttpScheme(String url) {
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        return lowerUrl.startsWith("http://http://")
                || lowerUrl.startsWith("http://https://")
                || lowerUrl.startsWith("https://http://")
                || lowerUrl.startsWith("https://https://");
    }

    private static int normalizePort(String scheme, int port) {
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            return -1;
        }
        return port;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }

        String normalizedPath = path;
        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return normalizedPath;
    }
}
