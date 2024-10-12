package org.service.notificationservice.service;

import org.service.notificationservice.event.NotificationEvent;

public interface NotificationService {

    void listen(NotificationEvent event, String topic);
}
