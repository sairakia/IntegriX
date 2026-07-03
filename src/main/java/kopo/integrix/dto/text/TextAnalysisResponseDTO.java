package kopo.integrix.dto.text;


/**
 * 텍스트 분석 응답 DTO입니다. 텍스트 위험 점수, 결과 라벨, 판단 근거, 상세 분석 항목을 프론트엔드에 전달합니다.
 */
import java.util.List;

public record TextAnalysisResponseDTO(
        String input,
        String trustLevel,
        int trustScore,
        int riskScore,
        Analysis analysis
) {
    public record Analysis(
            List<String> keyClaims,
            List<String> riskyExpressions,
            List<SentenceAnalysis> sentences,
            List<String> recommendations
    ) {
    }

    public record SentenceAnalysis(
            String text,
            String severity,
            String reason
    ) {
    }
}
