package org.service.notificationservice.event;

public record NotificationEvent(String subject, String message, String email) {
}
