package kopo.integrix.dto;

import java.time.LocalDateTime;

public record ReportFeedbackResponseDTO(
        Long feedbackId,
        String userId,
        String resultId,
        String feedbackType,
        String content,
        String reason,
        String status,
        String adminReply,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        LocalDateTime updatedAt
) {
}
