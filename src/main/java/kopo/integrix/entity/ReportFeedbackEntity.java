package kopo.integrix.entity;


/**
 * report_feedback 테이블과 매핑되는 JPA Entity입니다. 신고 URL, 신고 사유, 상세 설명, 신고한 사용자 정보를 저장합니다.
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "result_id", length = 50)
    private String resultId;

    @Column(name = "feedback_type", nullable = false, length = 20)
    private String feedbackType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ReportFeedbackEntity create(String userId, String resultId, String feedbackType, String content, String reason) {
        ReportFeedbackEntity entity = new ReportFeedbackEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.userId = userId;
        entity.resultId = resultId;
        entity.feedbackType = feedbackType;
        entity.content = content;
        entity.reason = reason;
        entity.status = "접수";
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void updateStatus(String status, String adminReply) {
        LocalDateTime now = LocalDateTime.now();
        this.status = status;
        this.adminReply = adminReply;
        this.updatedAt = now;
        this.processedAt = isClosedStatus(status) ? now : null;
    }

    private boolean isClosedStatus(String status) {
        return "완료".equals(status) || "반려".equals(status);
    }
}
