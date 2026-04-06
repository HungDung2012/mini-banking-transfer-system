package com.btlbanking.notification.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

  List<NotificationEntity> findTop20ByRecipientAccountOrderByCreatedAtDesc(String recipientAccount);
}
