package com.kshrd.kroya_api.repository.Notification;

import com.kshrd.kroya_api.entity.NotificationEntity;
import com.kshrd.kroya_api.entity.UserEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity,Long> {
    List<NotificationEntity> findByReceiverOrderByCreatedDateDesc(UserEntity receiver);
    NotificationEntity findByReceiverIdAndId(Integer userId,Long id);
}
