package org.service.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service.notificationservice.dto.*;
import org.service.notificationservice.entity.Email;
import org.service.notificationservice.entity.Reply;
import org.service.notificationservice.mapper.EmailMapper;
import org.service.notificationservice.repository.EmailRepository;
import org.service.notificationservice.repository.ReplyRepository;
import org.service.notificationservice.service.ContactService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final EmailRepository emailRepository;

    private final EmailMapper emailMapper;

    private final JavaMailSender javaMailSender;

    @Value("${email.from}")
    private String emailFrom;

    @Override
    public EmailResponse saveContactMessage(ContactRequest contactRequest) {
        Email newEmail = new Email();
        newEmail.setEmail(contactRequest.email());
        newEmail.setSubject(contactRequest.subject());
        newEmail.setBody(contactRequest.emailBody());
        newEmail.setTimestamp(LocalDateTime.now());
        newEmail.setStatus(Status.OPEN);
        newEmail.setUserId(contactRequest.userId());

       return emailMapper.mapToResponse(emailRepository.save(newEmail));
    }

    public List<EmailResponse> getEmails(String userId) {
        if (userId != null && !userId.isEmpty()) {
            return emailMapper.map(emailRepository.findAllByUserId(userId));
        }

        return emailMapper.map(emailRepository.findAll());
    }

    public EmailResponseWithReplies getEmail(Long id) {
        Email email = emailRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Email not found"));

        return emailMapper.map(email);
    }

    @Override
    public void reply(Long emailId, ContactRequest contactRequest) {
        Email email = emailRepository.findById(emailId).orElseThrow(
                () -> new RuntimeException("Email not found")
        );

        Reply reply = new Reply();
        reply.setBody(contactRequest.emailBody());
        reply.setTimestamp(LocalDateTime.now());
        reply.setFromEmail(contactRequest.email());

        if (email.getEmail().equals(contactRequest.email())) {
            reply.setEmailStatus(EmailStatus.CUSTOMER_REPLIED);
        } else {
            reply.setEmailStatus(EmailStatus.ADMIN_REPLIED);
        }

        reply.setEmail(email);
        email.getReplies().add(reply);

        emailRepository.save(email);

        //sendEmail(reply.getSubject(), reply.getBody(), emailFrom, reply.getFromEmail());
    }

    @Override
    public EmailResponse update(Long id, EmailUpdateRequest emailUpdateRequest) {
        Email email = emailRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Email not found"));

        email.setStatus(emailUpdateRequest.status());

        return emailMapper.mapToResponse(emailRepository.save(email));
    }

    @Override
    public void delete(Long id) {
        emailRepository.deleteById(id);
    }

    private void sendEmail(String subject, String body, String emailFrom, String emailTo) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(emailFrom);
            messageHelper.setTo(emailTo);
            messageHelper.setSubject(subject);
            messageHelper.setText(body);
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Contact email to {} was sent", emailTo);
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail", e);
        }
    }
}
