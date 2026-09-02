package kopo.integrix.dto;

public record NoticeRequestDTO(
        String title,
        String content,
        boolean pinned,
        boolean visible
) {
}
