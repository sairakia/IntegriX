package kopo.integrix.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "url_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UrlPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "list_type", nullable = false, length = 20)
    private String listType;

    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;

    @Column(name = "pattern", nullable = false, length = 500)
    private String pattern;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UrlPolicyEntity create(String listType, String matchType, String pattern, String reason, boolean active) {
        UrlPolicyEntity entity = new UrlPolicyEntity();
        entity.update(listType, matchType, pattern, reason, active);
        return entity;
    }

    public void update(String listType, String matchType, String pattern, String reason, boolean active) {
        this.listType = normalizeCode(listType);
        this.matchType = normalizeCode(matchType);
        this.pattern = pattern;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.active = active;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
