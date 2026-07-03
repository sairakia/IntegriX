package kopo.integrix.service;


/**
 * 외부 LLM 분석 실패를 표현하는 예외 클래스입니다. 타임아웃이나 외부 API 실패를 0점 결과로 숨기지 않고 실패 응답으로 처리할 때 사용합니다.
 */
public class ExternalAnalysisException extends RuntimeException {

    public ExternalAnalysisException(String message) {
        super(message);
    }
}
