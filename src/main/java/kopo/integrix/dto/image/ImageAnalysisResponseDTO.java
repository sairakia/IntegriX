package kopo.integrix.dto.image;


/**
 * 이미지 분석 응답 DTO입니다. 이미지 위험 점수, 결과 라벨, 판단 근거, 상세 분석 항목을 프론트엔드에 전달합니다.
 */
import java.util.List;

public record ImageAnalysisResponseDTO(
        String input,
        String credibility,
        int confidence,
        int riskScore,
        Analysis analysis
) {
    public record Analysis(
            List<String> metadata,
            List<String> manipulationIndicators,
            List<String> recommendations
    ) {
    }
}
