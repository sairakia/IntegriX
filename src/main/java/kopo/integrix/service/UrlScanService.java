package kopo.integrix.service;

/**
 * urlscan.io 기존 스캔 검색 기능의 Service 계약입니다.
 * URL이 urlscan.io의 공개/권한 범위 내 기존 분석 기록에서 악성으로 판단되었는지 확인합니다.
 */
public interface UrlScanService {

    UrlScanLookupResult lookupUrl(String url) throws Exception;

    record UrlScanLookupResult(
            boolean checked,
            boolean malicious,
            int score,
            int total,
            String scannedUrl,
            String scannedAt,
            String resultUrl,
            String message
    ) {
        public static UrlScanLookupResult skipped(String message) {
            return new UrlScanLookupResult(false, false, 0, 0, null, null, null, message);
        }

        public static UrlScanLookupResult noResult() {
            return new UrlScanLookupResult(true, false, 0, 0, null, null, null, "기존 분석 기록이 없습니다.");
        }
    }
}
