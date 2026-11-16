package com.washmycar.config;

import com.washmycar.model.Role;
import com.washmycar.model.StatutCompte;
import com.washmycar.model.Utilisateur;
import com.washmycar.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** RG12 : le compte ADMIN est créé à l'initialisation de la base, jamais par inscription publique. */
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UtilisateurRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminInitializer(UtilisateurRepository users, PasswordEncoder encoder,
                            @Value("${app.admin.email}") String email,
                            @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (users.existsByRole(Role.ADMIN)) return;
        Utilisateur a = new Utilisateur();
        a.setNom("Admin");
        a.setPrenom("Wash My Car");
        a.setEmail(email.toLowerCase());
        a.setMotDePasseHash(encoder.encode(password));
        a.setRole(Role.ADMIN);
        a.setStatutCompte(StatutCompte.ACTIF);
        users.save(a);
    }
}
