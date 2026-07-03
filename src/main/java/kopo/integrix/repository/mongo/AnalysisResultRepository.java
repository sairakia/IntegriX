package kopo.integrix.repository.mongo;


/**
 * MongoDB의 분석 결과 문서와 연결된 Repository입니다. 분석 기록 목록, 최근 분석, 대시보드 통계 조회에 사용됩니다.
 */
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnalysisResultRepository extends MongoRepository<AnalysisResultDTO, String> {

    long countByAnalysisTypeAndResultLabel(String analysisType, String resultLabel);

    long countByAnalysisTypeAndResultLabelIn(String analysisType, List<String> resultLabels);

    List<AnalysisResultDTO> findTop5ByOrderByCreatedAtDesc();

    long countByUserId(String userId);

    long countByUserIdAndAnalysisTypeAndResultLabel(String userId, String analysisType, String resultLabel);

    long countByUserIdAndAnalysisTypeAndResultLabelIn(String userId, String analysisType, List<String> resultLabels);

    List<AnalysisResultDTO> findByUserId(String userId);

    List<AnalysisResultDTO> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AnalysisResultDTO> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
}
