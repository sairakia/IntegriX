package kopo.integrix.service;


/**
 * Google Safe Browsing 조회 기능의 Service 계약입니다. URL이 외부 보안 DB에서 위험 URL로 판단되는지 확인하는 역할을 정의합니다.
 */
public interface SafeBrowsingService {
    boolean isUnsafeUrl(String url) throws Exception;
}