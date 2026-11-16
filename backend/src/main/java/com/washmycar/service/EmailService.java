package com.washmycar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String baseUrl;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.email.base-url}") String baseUrl,
                        @Value("${app.email.from}") String from) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    public void sendConfirmation(String to, String token) {
        String link = baseUrl + "/api/auth/verify-email?token=" + token;
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                log.info("SMTP non configuré. Lien de confirmation pour {} : {}", to, link);
                return;
            }
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject("Wash My Car - Confirmez votre compte");
            m.setText("Bienvenue sur Wash My Car !\n\nConfirmez votre adresse e-mail (lien valable 24 h) :\n" + link);
            sender.send(m);
        } catch (Exception e) {
            log.error("Échec d'envoi de l'e-mail à {} : {}", to, e.getMessage());
            log.info("Lien de confirmation pour {} : {}", to, link);
        }
    }
}
