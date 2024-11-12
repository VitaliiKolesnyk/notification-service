package org.service.notificationservice.repository;

import org.service.notificationservice.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findAllBySubjectAndFromEmail(String subject, String fromEmail);
}
