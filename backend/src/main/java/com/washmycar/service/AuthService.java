package com.washmycar.service;

import com.washmycar.dto.Dtos.*;
import com.washmycar.exception.ApiException;
import com.washmycar.model.*;
import com.washmycar.repository.JetonVerificationRepository;
import com.washmycar.repository.StationRepository;
import com.washmycar.repository.UtilisateurRepository;
import com.washmycar.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UtilisateurRepository users;
    private final StationRepository stations;
    private final JetonVerificationRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailService emailService;
    private final boolean emailEnabled;
    private final long expirationHours;

    public AuthService(UtilisateurRepository users, StationRepository stations,
                       JetonVerificationRepository tokens, PasswordEncoder encoder,
                       JwtService jwt, EmailService emailService,
                       @Value("${app.email.enabled}") boolean emailEnabled,
                       @Value("${app.verification.expiration-hours}") long expirationHours) {
        this.users = users;
        this.stations = stations;
        this.tokens = tokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.emailService = emailService;
        this.emailEnabled = emailEnabled;
        this.expirationHours = expirationHours;
    }

    // ---------- US01 + US20 : inscription Client ----------
    @Transactional
    public MessageResponse registerClient(ClientRegisterRequest r) {
        String email = normalize(r.email());
        checkEmailFree(email);

        StatutCompte statut = emailEnabled ? StatutCompte.EN_ATTENTE_EMAIL : StatutCompte.ACTIF;
        Utilisateur u = buildUser(r.nom(), r.prenom(), email, r.telephone(), r.motDePasse(), Role.CLIENT, statut);
        users.save(u);

        if (emailEnabled) {
            JetonVerification j = new JetonVerification();
            j.setUtilisateur(u);
            j.setJeton(UUID.randomUUID().toString());
            j.setDateExpiration(LocalDateTime.now().plusHours(expirationHours));   // RG09
            tokens.save(j);
            emailService.sendConfirmation(email, j.getJeton());
            return new MessageResponse("Compte créé : vérifiez votre e-mail");
        }
        return new MessageResponse("Compte créé");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        JetonVerification j = tokens.findByJeton(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Lien de confirmation invalide"));
        if (j.isUtilise() || j.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lien expiré ou déjà utilisé");   // RG09
        }
        j.setUtilise(true);
        j.getUtilisateur().setStatutCompte(StatutCompte.ACTIF);
        return new MessageResponse("Compte confirmé");
    }

    // ---------- US09 : inscription Station ----------
    @Transactional
    public MessageResponse registerStation(StationRegisterRequest r) {
        String email = normalize(r.email());
        checkEmailFree(email);

        Utilisateur u = buildUser(r.nom(), r.prenom(), email, r.telephone(), r.motDePasse(),
                Role.STATION, StatutCompte.ACTIF);
        users.save(u);

        Station s = new Station();
        s.setUtilisateur(u);
        s.setNomStation(r.nomStation().trim());
        s.setAdresse(r.adresse().trim());
        s.setDescription(r.description());
        s.setStatutValidation(StatutValidation.EN_ATTENTE);   // RG05
        stations.save(s);
        return new MessageResponse("Demande envoyée : en attente de validation par l'administrateur");
    }

    // ---------- US02 + US19 : connexion ----------
    public LoginResponse login(LoginRequest r) {
        Utilisateur u = users.findByEmail(normalize(r.email()))
                .filter(x -> encoder.matches(r.motDePasse(), x.getMotDePasseHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou mot de passe incorrect"));

        if (u.getRole() == Role.STATION) {                       // RG07
            Station s = stations.findByUtilisateurId(u.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Station introuvable"));
            switch (s.getStatutValidation()) {
                case EN_ATTENTE -> throw new ApiException(HttpStatus.FORBIDDEN,
                        "Votre demande est en attente de validation par l'administrateur");
                case REFUSEE -> throw new ApiException(HttpStatus.FORBIDDEN,
                        "Votre demande a été refusée. Motif : " + s.getMotifRefus());
                default -> { }
            }
        }
        if (u.getRole() == Role.CLIENT && u.getStatutCompte() != StatutCompte.ACTIF) {   // RG08
            throw new ApiException(HttpStatus.FORBIDDEN, "Veuillez confirmer votre e-mail avant de vous connecter");
        }
        return new LoginResponse(jwt.generate(u), u.getRole().name());
    }

    // ---------- utilitaires ----------
    private String normalize(String email) {                      // RG02
        return email.trim().toLowerCase();
    }

    private void checkEmailFree(String email) {
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "E-mail déjà utilisé");
        }
    }

    private Utilisateur buildUser(String nom, String prenom, String email, String tel,
                                  String mdp, Role role, StatutCompte statut) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom.trim());
        u.setPrenom(prenom.trim());
        u.setEmail(email);
        u.setTelephone(tel);
        u.setMotDePasseHash(encoder.encode(mdp));                 // RG04
        u.setRole(role);
        u.setStatutCompte(statut);
        return u;
    }
}
