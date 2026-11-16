package com.washmycar.service;

import com.washmycar.dto.*;
import com.washmycar.entity.Role;
import com.washmycar.entity.User;
import com.washmycar.exception.ApiException;
import com.washmycar.mapper.DtoMapper;
import com.washmycar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.washmycar.service.AuthService.blankToNull;
import static com.washmycar.service.AuthService.normalizeEmail;

/** Profil de l'utilisateur connecté et gestion des clients par l'administrateur. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ profil

    @Transactional(readOnly = true)
    public UserResponse getProfil(Long userId) {
        return DtoMapper.toUser(getUser(userId));
    }

    @Transactional
    public UserResponse updateProfil(Long userId, ProfileUpdateRequest req) {
        User user = getUser(userId);
        user.setNom(req.nom().trim());
        user.setPrenom(req.prenom().trim());
        user.setTelephone(blankToNull(req.telephone()));
        if (req.nouveauMotDePasse() != null && !req.nouveauMotDePasse().isBlank()) {
            if (req.ancienMotDePasse() == null
                    || !passwordEncoder.matches(req.ancienMotDePasse(), user.getMotDePasse())) {
                throw ApiException.badRequest("L'ancien mot de passe est incorrect");
            }
            user.setMotDePasse(passwordEncoder.encode(req.nouveauMotDePasse()));
        }
        return DtoMapper.toUser(user);
    }

    // ------------------------------------------------------------------ administration des clients

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchClients(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"));
        return PageResponse.of(userRepository.search(Role.CLIENT, blankToNull(q), pageable), DtoMapper::toUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getClient(Long id) {
        return DtoMapper.toUser(getClientEntity(id));
    }

    /** Un client créé par l'administrateur est considéré comme vérifié par défaut. */
    @Transactional
    public UserResponse createClient(ClientRequest req) {
        if (req.motDePasse() == null || req.motDePasse().isBlank()) {
            throw ApiException.badRequest("Le mot de passe est obligatoire");
        }
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        User user = User.builder()
                .nom(req.nom().trim())
                .prenom(req.prenom().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(blankToNull(req.telephone()))
                .role(Role.CLIENT)
                .emailVerifie(req.emailVerifie() == null || req.emailVerifie())
                .build();
        return DtoMapper.toUser(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateClient(Long id, ClientRequest req) {
        User user = getClientEntity(id);
        String email = normalizeEmail(req.email());
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        user.setNom(req.nom().trim());
        user.setPrenom(req.prenom().trim());
        user.setEmail(email);
        user.setTelephone(blankToNull(req.telephone()));
        if (req.motDePasse() != null && !req.motDePasse().isBlank()) {
            user.setMotDePasse(passwordEncoder.encode(req.motDePasse()));
        }
        if (req.emailVerifie() != null) {
            user.setEmailVerifie(req.emailVerifie());
            if (req.emailVerifie()) {
                user.setTokenVerification(null);
                user.setTokenExpiration(null);
            }
        }
        return DtoMapper.toUser(user);
    }

    /** Supprime le client ainsi que ses réservations et évaluations (cascade en base). */
    @Transactional
    public void deleteClient(Long id) {
        getClientEntity(id);
        userRepository.hardDelete(id);
    }

    // ------------------------------------------------------------------ utilitaires

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private User getClientEntity(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CLIENT)
                .orElseThrow(() -> ApiException.notFound("Client introuvable"));
    }
}
