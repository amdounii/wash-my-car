package com.washmycar.repository;

import com.washmycar.entity.Role;
import com.washmycar.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByTokenVerification(String token);

    long countByRole(Role role);

    boolean existsByRole(Role role);

    /** Recherche paginée par rôle, filtrée sur nom, prénom, email ou téléphone. */
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (:q IS NULL
                   OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR u.telephone LIKE CONCAT('%', :q, '%'))
            """)
    Page<User> search(@Param("role") Role role, @Param("q") String q, Pageable pageable);

    /**
     * Suppression directe en SQL : les tables liées (station, réservations,
     * évaluations…) sont supprimées par les ON DELETE CASCADE de la base.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM User u WHERE u.id = :id")
    int hardDelete(@Param("id") Long id);
}
