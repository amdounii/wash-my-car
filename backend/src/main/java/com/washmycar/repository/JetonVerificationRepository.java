package com.washmycar.repository;

import com.washmycar.model.JetonVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JetonVerificationRepository extends JpaRepository<JetonVerification, Long> {
    Optional<JetonVerification> findByJeton(String jeton);
}
