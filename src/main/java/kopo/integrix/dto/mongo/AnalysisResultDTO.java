package kopo.integrix.dto.mongo;


/**
 * MongoDB에 저장되는 분석 결과 문서 DTO입니다. 분석 타입, 대상 URL/텍스트/이미지, 위험 점수, 결과 라벨, 원본 응답을 저장합니다.
 */
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "ANALYSIS_RESULT")
public record AnalysisResultDTO(
        @Id
        String resultId,
        String userId,
        String analysisType,
        String inputData,
        Integer score,
        String resultLabel,
        String summary,
        Date createdAt,
        Object rawResult
) {
}
