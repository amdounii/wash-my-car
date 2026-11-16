package com.washmycar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

/** Authentification gérée par JWT : pas d'utilisateur en mémoire généré par Spring Boot. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class WashMyCarApplication {

    public static void main(String[] args) {
        // Toutes les dates (créneaux, horaires) sont exprimées en heure de Tunisie.
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Tunis"));
        SpringApplication.run(WashMyCarApplication.class, args);
    }
}
