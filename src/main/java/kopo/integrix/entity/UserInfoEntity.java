package kopo.integrix.entity;


/**
 * user_info 테이블과 매핑되는 JPA Entity입니다. 사용자 ID, 이메일, 암호화된 비밀번호, 권한, 상태, 프로필 이미지 URL을 저장합니다.
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfoEntity {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    private UserInfoEntity(String userId, String email, String password, String name, String role, String status) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public static UserInfoEntity createUser(String userId, String email, String encodedPassword, String name) {
        return new UserInfoEntity(userId, email, encodedPassword, name, "USER", "ACTIVE");
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate() {
        this.status = "INACTIVE";
    }

    public void reactivate(String email, String encodedPassword, String name) {
        this.email = email;
        this.password = encodedPassword;
        this.name = name;
        this.status = "ACTIVE";
        this.profileImageUrl = null;
    }
}
