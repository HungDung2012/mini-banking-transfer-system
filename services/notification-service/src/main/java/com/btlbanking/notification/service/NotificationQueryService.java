package com.btlbanking.notification.service;

import com.btlbanking.notification.domain.NotificationRepository;
import com.btlbanking.notification.web.NotificationResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationQueryService {

  private final NotificationRepository notificationRepository;

  public NotificationQueryService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public List<NotificationResponse> listForRecipient(String accountNumber) {
    return notificationRepository.findTop20ByRecipientAccountOrderByCreatedAtDesc(accountNumber)
        .stream()
        .map(NotificationResponse::from)
        .toList();
  }
}
