package kopo.integrix.dto;

public record ReportStatusUpdateRequestDTO(
        String status,
        String adminReply
) {
}
