package com.btlbanking.notification.web;

import com.btlbanking.notification.service.NotificationQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationQueryService notificationQueryService;

  public NotificationController(NotificationQueryService notificationQueryService) {
    this.notificationQueryService = notificationQueryService;
  }

  @GetMapping("/account/{accountNumber}")
  public List<NotificationResponse> listByAccount(
      @PathVariable("accountNumber") String accountNumber) {
    return notificationQueryService.listForAccount(accountNumber);
  }
}
