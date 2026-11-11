package com.cloudnest.academy.service;

import com.cloudnest.academy.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Sends email through SMTP (e.g. Amazon SES). When MAIL_HOST is not configured, emails are written to the log. */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final ObjectProvider<JavaMailSender> sender;
    private final AppConfig cfg;

    public MailService(ObjectProvider<JavaMailSender> sender, AppConfig cfg) {
        this.sender = sender;
        this.cfg = cfg;
    }

    public void send(String to, String subject, String body) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.info("[EMAIL not sent - MAIL_HOST not configured] to={} subject='{}'\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(cfg.mailFrom);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body + "\n\n— " + cfg.name + "\n" + cfg.frontendUrl);
            s.send(m);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }
}
