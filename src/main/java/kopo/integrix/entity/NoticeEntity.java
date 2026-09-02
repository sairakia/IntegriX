package kopo.integrix.entity;

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
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static NoticeEntity create(String title, String content, boolean pinned, boolean visible, String createdBy) {
        NoticeEntity entity = new NoticeEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.title = title;
        entity.content = content;
        entity.pinned = pinned;
        entity.visible = visible;
        entity.createdBy = createdBy;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void update(String title, String content, boolean pinned, boolean visible) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.visible = visible;
        this.updatedAt = LocalDateTime.now();
    }
}
