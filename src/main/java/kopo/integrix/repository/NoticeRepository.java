package kopo.integrix.repository;

import kopo.integrix.entity.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {

    List<NoticeEntity> findByVisibleTrueOrderByPinnedDescCreatedAtDesc();

    List<NoticeEntity> findAllByOrderByPinnedDescCreatedAtDesc();
}
