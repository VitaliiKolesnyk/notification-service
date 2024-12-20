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

        log.info("Received message from Kafka topic '{}': {}", topic, message);

        switch (topic) {
            case "order-topic":
                OrderEvent orderEvent = deserialize(message, OrderEvent.class);
                log.info("Deserialized OrderEvent: {}", orderEvent);
                handleOrderPlacedEvent(orderEvent);
                break;
            case "inventory-limit-topic":
                LimitExceedEvent limitExceedEvent = deserialize(message, LimitExceedEvent.class);
                log.info("Deserialized LimitExceedEvent: {}", limitExceedEvent);
                handleLimitExceedEvent(limitExceedEvent);
                break;
            case "payment-events":
                PaymentEvent paymentEvent = deserialize(message, PaymentEvent.class);
                log.info("Deserialized PaymentEvent: {}", paymentEvent);
                handlePaymentEvent(paymentEvent);
                break;
            default:
                log.error("Unknown topic: {}", topic);
                throw new IllegalArgumentException("Unknown topic: " + topic);
        }
    }

    private void handleOrderPlacedEvent(OrderEvent orderEvent) {
        log.info("Handling OrderPlacedEvent for order number: {}", orderEvent.getOrderNumber());

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(emailFrom);
            messageHelper.setTo(orderEvent.getEmail());
            messageHelper.setSubject(String.format("Order %s %s", orderEvent.getOrderNumber(), orderEvent.getStatus().equals("Placed") ? "received" : "cancelled"));
            String body = orderEvent.getStatus().equals("Placed")
                    ? String.format("Your Order %s was received and currently is being processed.", orderEvent.getOrderNumber())
                    : String.format("Your Order %s was cancelled", orderEvent.getOrderNumber());

            messageHelper.setText(String.format("""
                    Dear %s,
                    
                    %s
                    
                    Thank you.
                    
                    Best regards,
                    AppleShop
                    """, orderEvent.getName(), body));
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Order Placed Notification email to {} was sent", orderEvent.getEmail());
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail", e);
        }
    }

    private void handleLimitExceedEvent(LimitExceedEvent limitExceedEvent) {
        log.info("Handling LimitExceedEvent for SKU code: {}", limitExceedEvent.getSkuCode());

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

    private void handlePaymentEvent(PaymentEvent paymentEvent) {
        log.info("Handling PaymentEvent for order number: {}", paymentEvent.orderNumber());

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
            T result = objectMapper.readValue(json, targetType);
            log.info("Deserialized message to {}: {}", targetType.getSimpleName(), result);
            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON message: {}", json, e);
            throw new RuntimeException("Failed to deserialize JSON message", e);
        }
    }
}
