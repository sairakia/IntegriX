package kopo.integrix.repository;


/**
 * ReportFeedbackEntity와 연결된 JPA Repository입니다. 사용자가 신고한 URL 데이터를 저장하고 분석 점수에 신고 이력을 반영할 때 조회합니다.
 */
import kopo.integrix.entity.ReportFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportFeedbackRepository extends JpaRepository<ReportFeedbackEntity, Long> {

    // URL 분석 서비스가 신고 DB에 같은 URL이 있는지 빠르게 확인할 때 사용합니다.
    boolean existsByFeedbackTypeAndContent(String feedbackType, String content);

    List<ReportFeedbackEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ReportFeedbackEntity> findAllByOrderByCreatedAtDesc();
}
