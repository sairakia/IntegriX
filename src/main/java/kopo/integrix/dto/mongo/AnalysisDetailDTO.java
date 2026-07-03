package kopo.integrix.dto.mongo;


/**
 * MongoDB에 저장되는 분석 상세 문서 DTO입니다. 분석 결과를 다시 열었을 때 원래 분석 화면처럼 보여줄 세부 데이터를 보관합니다.
 */
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "ANALYSIS_DETAIL")
public record AnalysisDetailDTO(
        @Id
        String detailId,
        String resultId,
        String issueContent,
        String severity,
        Date createdAt
) {
}
