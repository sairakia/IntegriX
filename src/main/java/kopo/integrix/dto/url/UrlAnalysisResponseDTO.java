package kopo.integrix.dto.url;


/**
 * URL 분석 응답 DTO입니다. 위험 점수, 신뢰 수준, 점수 반영 요소, 상세 검사 결과를 프론트엔드에 전달합니다.
 */
import java.util.List;

public record UrlAnalysisResponseDTO(
        String url,
        String trustLevel,
        int riskScore,
        List<ScoreFactor> scoreFactors,
        Analysis analysis
) {
    public record ScoreFactor(
            String label,
            int score,
            String reason
    ) {
    }

    public record Analysis(
            List<String> httpsConnection,
            List<String> sslCertificate,
            List<String> domainAge,
            List<String> blacklistStatus,
            List<String> recommendations
    ) {
    }
}
