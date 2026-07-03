package kopo.integrix.repository;


/**
 * UserInfoEntity와 연결된 JPA Repository입니다. 사용자 ID, 이메일 중복 확인과 로그인 사용자 조회에 필요한 findBy, existsBy 메서드를 제공합니다.
 */
import kopo.integrix.entity.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, String> {

    Optional<UserInfoEntity> findByUserId(String userId);

    Optional<UserInfoEntity> findByEmail(String email);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByUserIdAndStatus(String userId, String status);

    boolean existsByEmailAndStatus(String email, String status);
}
