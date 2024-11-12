package org.service.notificationservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.service.notificationservice.event.LimitExceedEvent;
import org.service.notificationservice.event.OrderEvent;
import org.service.notificationservice.event.PaymentEvent;
import org.service.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender javaMailSender;

    @Value("${admin.user.email}")
    private String adminUserEmail;

    @Value("${email.from}")
    private String emailFrom;

    @KafkaListener(topics = {"order-topic", "inventory-limit-topic", "payment-events"}, groupId = "notification-service-group")
    public void listen(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String message = record.value();

        switch (topic) {
            case "order-topic":
                OrderEvent orderEvent = deserialize(message, OrderEvent.class);
                handleOrderPlacedEvent(orderEvent);
                break;
            case "inventory-limit-topic":
                LimitExceedEvent limitExceedEvent = deserialize(message, LimitExceedEvent.class);
                handleLimitExceedEvent(limitExceedEvent);
                break;
            case "payment-events":
                PaymentEvent paymentEvent = deserialize(message, PaymentEvent.class);
                handlePaymentEvent(paymentEvent);
                break;
            default:
                throw new IllegalArgumentException("Unknown topic: " + topic);
        }
    }

    private void handleOrderPlacedEvent(OrderEvent orderEvent){
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(emailFrom);
            messageHelper.setTo(orderEvent.getEmail());
            messageHelper.setSubject(String.format("Order %s received", orderEvent.getOrderNumber()));
            messageHelper.setText(String.format("""
                    Dear %s,
                    
                    Your Order %s was received and currently is being processed.
                    
                    Thank you.
                    
                    Best regards,
                    AppleShop
                    """, orderEvent.getName(), orderEvent.getOrderNumber()));
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Order Placed Notification email to {} was sent", orderEvent.getEmail());
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail", e);
        }
    }

    private void handleLimitExceedEvent(LimitExceedEvent limitExceedEvent){
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(emailFrom);
            messageHelper.setTo(adminUserEmail);
            messageHelper.setSubject(String.format("Product %s limit exceed", limitExceedEvent.getSkuCode()));
            messageHelper.setText(String.format("""
                    Dear Admin,
                    
                    Please be informed that current quantity of product %s is less that limit %d.
                    
                    Thank you.
                    
                    Best regards,
                    AppleShop
                    """, limitExceedEvent.getSkuCode(), limitExceedEvent.getLimit()));
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Limit Exceeds Notification email to {} was sent", limitExceedEvent.getEmail());
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail to springshop@email.com", e);
        }
    }

    private void handlePaymentEvent(PaymentEvent paymentEvent){
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(emailFrom);
            messageHelper.setTo(paymentEvent.email());
            messageHelper.setSubject(String.format("Payment for order %s was %s", paymentEvent.orderNumber(), paymentEvent.status().toLowerCase()));
            messageHelper.setText(String.format("""
                    Dear Customer,
                    
                    Payment for order %s status is %s
                    
                    Thank you.
                    
                    Best regards,
                    AppleShop
                    """, paymentEvent.orderNumber(), paymentEvent.status().toLowerCase()));
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Payment Status Notification email to {} was sent", paymentEvent.email());
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail to springshop@email.com", e);
        }
    }

    private <T> T deserialize(String json, Class<T> targetType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON message", e);
        }
    }
}
