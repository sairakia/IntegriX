package kopo.integrix.repository;

import kopo.integrix.entity.UrlPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UrlPolicyRepository extends JpaRepository<UrlPolicyEntity, Long> {

    List<UrlPolicyEntity> findAllByOrderByUpdatedAtDescPolicyIdDesc();

    List<UrlPolicyEntity> findByActiveTrueOrderByUpdatedAtDescPolicyIdDesc();
}
