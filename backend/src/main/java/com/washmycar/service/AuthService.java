package com.washmycar.service;

import com.washmycar.dto.*;
import com.washmycar.entity.*;
import com.washmycar.exception.ApiException;
import com.washmycar.repository.StationRepository;
import com.washmycar.repository.UserRepository;
import com.washmycar.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** Inscription, validation de l'email et connexion. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int TOKEN_VALIDITE_HEURES = 24;

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final StationService stationService;

    /** Résultat de la validation d'un lien envoyé par email. */
    public enum VerificationResult { VALIDE, DEJA_VALIDE, EXPIRE, INVALIDE }

    /** Inscription client : le compte reste inactif jusqu'au clic sur le lien reçu par email. */
    @Transactional
    public MessageResponse registerClient(RegisterClientRequest req) {
        String email = normalizeEmail(req.email());
        ensureEmailLibre(email);
        User user = User.builder()
                .nom(req.nom().trim())
                .prenom(req.prenom().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(blankToNull(req.telephone()))
                .role(Role.CLIENT)
                .emailVerifie(false)
                .build();
        nouveauToken(user);
        userRepository.save(user);
        mailService.sendVerification(user);
        return new MessageResponse("Inscription réussie. Un email de validation a été envoyé à " + email + ".");
    }

    /** Inscription station : la connexion n'est possible qu'après validation par l'administrateur. */
    @Transactional
    public MessageResponse registerStation(RegisterStationRequest req) {
        String email = normalizeEmail(req.email());
        ensureEmailLibre(email);
        User user = userRepository.save(User.builder()
                .nom(req.nom().trim())
                .prenom(req.prenom().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(blankToNull(req.telephone()))
                .role(Role.STATION)
                .emailVerifie(true) // la validation se fait par l'administrateur
                .build());
        stationService.creerStation(user, req.station(), StatutInscription.EN_ATTENTE);
        return new MessageResponse("Inscription enregistrée. Votre station sera visible après validation par l'administrateur.");
    }

    @Transactional
    public VerificationResult verifyEmail(String token) {
        if (token == null || token.isBlank()) return VerificationResult.INVALIDE;
        return userRepository.findByTokenVerification(token)
                .map(user -> {
                    if (user.isEmailVerifie()) return VerificationResult.DEJA_VALIDE;
                    if (user.getTokenExpiration() != null && user.getTokenExpiration().isBefore(LocalDateTime.now())) {
                        return VerificationResult.EXPIRE;
                    }
                    user.setEmailVerifie(true);
                    user.setTokenVerification(null);
                    user.setTokenExpiration(null);
                    return VerificationResult.VALIDE;
                })
                .orElse(VerificationResult.INVALIDE);
    }

    /** Renvoie un nouveau lien. Réponse identique que l'email existe ou non (pas de fuite d'information). */
    @Transactional
    public MessageResponse resendVerification(String rawEmail) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(u -> u.getRole() == Role.CLIENT && !u.isEmailVerifie())
                .ifPresent(u -> {
                    nouveauToken(u);
                    mailService.sendVerification(u);
                });
        return new MessageResponse("Si un compte non validé existe pour cet email, un nouveau lien a été envoyé.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(req.email()))
                .filter(u -> passwordEncoder.matches(req.motDePasse(), u.getMotDePasse()))
                .orElseThrow(() -> ApiException.unauthorized("Email ou mot de passe incorrect"));

        Long stationId = null;
        switch (user.getRole()) {
            case CLIENT -> {
                if (!user.isEmailVerifie()) {
                    throw ApiException.forbidden(
                            "Votre compte n'est pas encore validé. Cliquez sur le lien reçu par email.");
                }
            }
            case STATION -> {
                Station station = stationRepository.findByUserId(user.getId())
                        .orElseThrow(() -> ApiException.forbidden("Aucune station associée à ce compte"));
                switch (station.getStatutInscription()) {
                    case EN_ATTENTE -> throw ApiException.forbidden(
                            "Votre inscription est en attente de validation par l'administrateur.");
                    case REFUSEE -> throw ApiException.forbidden("Votre inscription a été refusée par l'administrateur.");
                    case VALIDEE -> stationId = station.getId();
                }
            }
            case ADMIN -> { /* aucune condition supplémentaire */ }
        }
        return new AuthResponse(jwtService.generate(user), "Bearer", user.getId(), user.getNom(), user.getPrenom(),
                user.getEmail(), user.getRole(), stationId);
    }

    // ------------------------------------------------------------------ utilitaires

    void ensureEmailLibre(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
    }

    private void nouveauToken(User user) {
        user.setTokenVerification(UUID.randomUUID().toString());
        user.setTokenExpiration(LocalDateTime.now().plusHours(TOKEN_VALIDITE_HEURES));
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
