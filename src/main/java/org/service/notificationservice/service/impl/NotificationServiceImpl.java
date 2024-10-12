package org.service.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service.notificationservice.event.NotificationEvent;
import org.service.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender javaMailSender;

    @Value("${admin.user.email}")
    private String adminUserEmail;

    @KafkaListener(topics = {"order-placed-topic", "inventory-limit-topic"})
    public void listen(NotificationEvent notificationEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic){
        log.info("Got Message from {} {}", topic, notificationEvent);

        if (topic.equals("order-placed-topic")) {
            handleOrderPlacesEvent(notificationEvent);
        } else {
            handleInventoryLimitEvent(notificationEvent);
        }
    }

    public void handleInventoryLimitEvent(NotificationEvent notificationEvent) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("springshop@email.com");
            messageHelper.setTo(adminUserEmail);
            messageHelper.setSubject(notificationEvent.subject());
            messageHelper.setText(notificationEvent.message());
        };
            try {
                javaMailSender.send(messagePreparator);
                log.info("Limit Notifcation email sent!!");
            } catch (MailException e) {
                log.error("Exception occurred when sending mail", e);
                throw new RuntimeException("Exception occurred when sending mail to springshop@email.com", e);
            }
        }


    public void handleOrderPlacesEvent(NotificationEvent notificationEvent){
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("springshop@email.com");
            messageHelper.setTo(notificationEvent.email());
            messageHelper.setSubject(notificationEvent.subject());
            messageHelper.setText(notificationEvent.message());
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Order Placed Notifcation email sent!!");
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail to springshop@email.com", e);
        }
    }
}
