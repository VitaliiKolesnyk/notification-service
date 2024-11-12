package org.service.notificationservice.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface NotificationService {

    void listen(ConsumerRecord<String, String> record);

}
