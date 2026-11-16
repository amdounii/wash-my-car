package com.washmycar.security;

import com.washmycar.entity.StatutInscription;
import com.washmycar.entity.User;
import com.washmycar.repository.StationRepository;
import com.washmycar.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit l'en-tête « Authorization: Bearer &lt;jeton&gt; », vérifie le jeton puis
 * l'état du compte (toujours existant, email vérifié, station validée)
 * avant d'authentifier la requête.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parse(header.substring(BEARER.length()))
                    .map(claims -> Long.valueOf(claims.getSubject()))
                    .flatMap(userRepository::findById)
                    .filter(this::compteActif)
                    .ifPresent(user -> authenticate(user, request));
        }
        chain.doFilter(request, response);
    }

    /** Un compte supprimé, non vérifié ou une station non validée perd immédiatement l'accès. */
    private boolean compteActif(User user) {
        return switch (user.getRole()) {
            case ADMIN -> true;
            case CLIENT -> user.isEmailVerifie();
            case STATION -> stationRepository.findByUserId(user.getId())
                    .map(s -> s.getStatutInscription() == StatutInscription.VALIDEE)
                    .orElse(false);
        };
    }

    private void authenticate(User user, HttpServletRequest request) {
        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
