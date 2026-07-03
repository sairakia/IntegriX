package kopo.integrix.service;


/**
 * URL 분석 기능의 Service 계약입니다. Controller가 구체 구현을 모르고 URL 분석 요청을 위임할 수 있게 합니다.
 */
import kopo.integrix.dto.url.UrlAnalysisResponseDTO;

public interface UrlAnalysisService {
    UrlAnalysisResponseDTO analyzeUrl(String url, String userId) throws Exception;
}
