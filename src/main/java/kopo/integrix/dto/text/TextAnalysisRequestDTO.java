package kopo.integrix.dto.text;


/**
 * 텍스트 분석 요청 DTO입니다. 사용자가 분석하려는 텍스트 내용을 Controller와 Service로 전달합니다.
 */
public record TextAnalysisRequestDTO(
        String text
) {
}
