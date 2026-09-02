package kopo.integrix.dto;

import java.time.LocalDateTime;

public record NoticeResponseDTO(
        Long noticeId,
        String title,
        String content,
        boolean pinned,
        boolean visible,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
