package kopo.integrix.dto.url;


/**
 * URL 분석 요청 DTO입니다. 사용자가 분석하려는 url 값을 담아 Controller와 Service로 전달합니다.
 */
public record UrlAnalysisRequestDTO(String url) {
}