package kopo.integrix.dto.history;


/**
 * 분석 기록 목록 항목 DTO입니다. 마이페이지 분석 기록에서 한 건의 분석 결과를 표시하는 데 필요한 값을 담습니다.
 */
import java.util.List;

public record HistoryItemDTO(
        String id,
        String type,
        String input,
        String result,
        String date,
        Integer score,
        HistoryDetailsDTO details,
        Object rawResult
) {
    public record HistoryDetailsDTO(
            UrlAnalysisHistoryDTO urlAnalysis,
            TextAnalysisHistoryDTO textAnalysis,
            ImageAnalysisHistoryDTO imageAnalysis
    ) {
    }

    public record UrlAnalysisHistoryDTO(
            List<String> issues
    ) {
    }

    public record TextAnalysisHistoryDTO(
            String fullText,
            List<String> keyClaims,
            List<String> riskyExpressions,
            List<SentenceHistoryDTO> highlightedSentences
    ) {
    }

    public record SentenceHistoryDTO(
            String text,
            String severity
    ) {
    }

    public record ImageAnalysisHistoryDTO(
            String fileName,
            ImageMetadataHistoryDTO metadata,
            List<String> manipulationIndicators
    ) {
    }

    public record ImageMetadataHistoryDTO(
            String editingSoftware,
            String gpsLocation,
            String creationDate
    ) {
    }
}
