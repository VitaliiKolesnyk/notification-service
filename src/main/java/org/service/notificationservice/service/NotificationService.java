package org.service.notificationservice.service;

import org.service.notificationservice.event.OrderPlacedEvent;

public interface NotificationService {

    void listen(OrderPlacedEvent orderPlacedEvent);
}
