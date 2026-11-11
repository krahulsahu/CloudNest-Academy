package com.cloudnest.academy.service;

import com.cloudnest.academy.domain.Notification;
import com.cloudnest.academy.domain.NotificationRepository;
import com.cloudnest.academy.domain.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final MailService mail;

    public NotificationService(NotificationRepository repo, MailService mail) {
        this.repo = repo;
        this.mail = mail;
    }

    /** In-app notification plus an optional email. */
    public void notify(User user, String title, String body, String link, boolean email) {
        Notification n = new Notification();
        n.user = user;
        n.title = title;
        n.body = body;
        n.link = link;
        repo.save(n);
        if (email) mail.send(user.email, title, "Hi " + user.name + ",\n\n" + body);
    }
}
