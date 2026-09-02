package kopo.integrix.controller;

import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.NoticeRequestDTO;
import kopo.integrix.dto.NoticeResponseDTO;
import kopo.integrix.entity.NoticeEntity;
import kopo.integrix.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeRepository noticeRepository;

    @GetMapping
    public CommonResponseDTO getVisibleNotices() {
        return new CommonResponseDTO(
                true,
                "공지사항 목록입니다.",
                noticeRepository.findByVisibleTrueOrderByPinnedDescCreatedAtDesc()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @GetMapping("/{noticeId}")
    public CommonResponseDTO getNotice(@PathVariable Long noticeId) {
        NoticeEntity notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null || !notice.isVisible()) {
            return new CommonResponseDTO(false, "공지사항을 찾을 수 없습니다.", null);
        }

        return new CommonResponseDTO(true, "공지사항입니다.", toResponse(notice));
    }

    @GetMapping("/admin/list")
    public CommonResponseDTO getAllNotices() {
        return new CommonResponseDTO(
                true,
                "관리자 공지사항 목록입니다.",
                noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @PostMapping("/admin")
    public CommonResponseDTO createNotice(@RequestBody NoticeRequestDTO request,
                                          @AuthenticationPrincipal String userId) {
        String validationMessage = validateRequest(request);
        if (validationMessage != null) {
            return new CommonResponseDTO(false, validationMessage, null);
        }

        NoticeEntity notice = noticeRepository.save(NoticeEntity.create(
                request.title().trim(),
                request.content().trim(),
                request.pinned(),
                request.visible(),
                userId
        ));

        return new CommonResponseDTO(true, "공지사항이 등록되었습니다.", toResponse(notice));
    }

    @PatchMapping("/admin/{noticeId}")
    public CommonResponseDTO updateNotice(@PathVariable Long noticeId,
                                          @RequestBody NoticeRequestDTO request) {
        String validationMessage = validateRequest(request);
        if (validationMessage != null) {
            return new CommonResponseDTO(false, validationMessage, null);
        }

        NoticeEntity notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            return new CommonResponseDTO(false, "공지사항을 찾을 수 없습니다.", null);
        }

        notice.update(request.title().trim(), request.content().trim(), request.pinned(), request.visible());
        noticeRepository.save(notice);
        return new CommonResponseDTO(true, "공지사항이 수정되었습니다.", toResponse(notice));
    }

    @DeleteMapping("/admin/{noticeId}")
    public CommonResponseDTO deleteNotice(@PathVariable Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            return new CommonResponseDTO(false, "공지사항을 찾을 수 없습니다.", null);
        }

        noticeRepository.deleteById(noticeId);
        return new CommonResponseDTO(true, "공지사항이 삭제되었습니다.", null);
    }

    private String validateRequest(NoticeRequestDTO request) {
        if (request == null || isBlank(request.title()) || isBlank(request.content())) {
            return "제목과 내용을 입력해 주세요.";
        }
        if (request.title().trim().length() > 200) {
            return "제목은 200자 이하로 입력해 주세요.";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private NoticeResponseDTO toResponse(NoticeEntity notice) {
        return new NoticeResponseDTO(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.isVisible(),
                notice.getCreatedBy(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
