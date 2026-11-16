package com.washmycar.config;

import com.washmycar.entity.Role;
import com.washmycar.entity.User;
import com.washmycar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Crée un compte administrateur au démarrage si ADMIN_EMAIL et ADMIN_PASSWORD
 * sont définis et qu'aucun compte n'existe avec cet email.
 * Utile lorsque la base est créée sans le jeu de données data.sql.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            if (!userRepository.existsByRole(Role.ADMIN)) {
                log.warn("Aucun administrateur en base : importez database/data.sql "
                        + "ou définissez ADMIN_EMAIL et ADMIN_PASSWORD.");
            }
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            return;
        }
        userRepository.save(User.builder()
                .nom("Admin")
                .prenom("Wash My Car")
                .email(normalized)
                .motDePasse(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .emailVerifie(true)
                .build());
        log.info("Compte administrateur créé : {}", normalized);
    }
}
