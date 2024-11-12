package org.service.notificationservice.dto;

import org.service.notificationservice.entity.Reply;

import java.time.LocalDateTime;
import java.util.List;

public record EmailResponseWithReplies(Long id, String subject, String body, String email, LocalDateTime timestamp, List<ReplyResponse> replies) {
}
