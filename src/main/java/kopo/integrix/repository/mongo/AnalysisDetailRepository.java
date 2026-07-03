package kopo.integrix.repository.mongo;


/**
 * MongoDB의 분석 상세 문서와 연결된 Repository입니다. 분석 결과의 원본 상세 항목이나 화면 재구성용 상세 데이터를 저장하고 조회합니다.
 */
import kopo.integrix.dto.mongo.AnalysisDetailDTO;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisDetailRepository extends MongoRepository<AnalysisDetailDTO, String> {

    List<AnalysisDetailDTO> findByResultId(String resultId);
}
