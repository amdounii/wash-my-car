package com.washmycar.service;

import com.washmycar.entity.Station;
import com.washmycar.entity.StatutInscription;
import com.washmycar.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi des emails transactionnels.
 * Si aucun serveur SMTP n'est configuré (MAIL_HOST vide), le contenu est écrit
 * dans les logs : pratique pour tester la validation de compte en local.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    @Value("${app.mail.from:no-reply@washmycar.tn}")
    private String from;

    public void sendVerification(User user) {
        String link = publicBaseUrl + "/api/auth/verify?token=" + user.getTokenVerification();
        String body = """
                Bonjour %s,

                Merci pour votre inscription sur Wash My Car.
                Pour activer votre compte, cliquez sur le lien suivant (valable 24 heures) :

                %s

                Si vous n'êtes pas à l'origine de cette inscription, ignorez cet email.

                L'équipe Wash My Car
                """.formatted(user.getPrenom(), link);
        send(user.getEmail(), "Wash My Car - Validez votre compte", body);
    }

    public void sendStatutStation(Station station) {
        boolean validee = station.getStatutInscription() == StatutInscription.VALIDEE;
        String body = """
                Bonjour %s,

                %s

                L'équipe Wash My Car
                """.formatted(station.getUser().getPrenom(), validee
                ? "Bonne nouvelle : votre station « " + station.getNom()
                  + " » a été validée. Vous pouvez désormais vous connecter à l'application."
                : "Nous sommes désolés : l'inscription de votre station « " + station.getNom()
                  + " » a été refusée. Contactez-nous pour plus d'informations.");
        send(station.getUser().getEmail(),
                "Wash My Car - Inscription " + (validee ? "validée" : "refusée"), body);
    }

    /** Un échec d'envoi n'annule pas l'opération métier : il est seulement journalisé. */
    private void send(String to, String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("""

                    ===== EMAIL (SMTP non configuré) =====
                    À : {}
                    Objet : {}
                    {}
                    ======================================""", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
        } catch (MailException e) {
            log.error("Échec de l'envoi de l'email à {} : {}", to, e.getMessage());
            log.info("Contenu non envoyé :\n{}", body);
        }
    }
}
