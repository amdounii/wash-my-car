package com.washmycar.repository;

import com.washmycar.entity.Reservation;
import com.washmycar.entity.StatutReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Réservations bloquantes qui chevauchent l'intervalle [debut, fin[ (hors réservation exclue). */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.station.id = :stationId
              AND r.statut IN :statuts
              AND r.dateDebut < :fin
              AND r.dateFin > :debut
              AND (:exclureId IS NULL OR r.id <> :exclureId)
            """)
    boolean existsChevauchement(@Param("stationId") Long stationId,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin,
                                @Param("statuts") Collection<StatutReservation> statuts,
                                @Param("exclureId") Long exclureId);

    /** Créneaux occupés d'une station sur une période (affichage de la disponibilité). */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.station.id = :stationId
              AND r.statut IN :statuts
              AND r.dateDebut < :fin
              AND r.dateFin > :debut
            ORDER BY r.dateDebut
            """)
    List<Reservation> findOccupees(@Param("stationId") Long stationId,
                                   @Param("debut") LocalDateTime debut,
                                   @Param("fin") LocalDateTime fin,
                                   @Param("statuts") Collection<StatutReservation> statuts);

    @EntityGraph(attributePaths = {"station", "client"})
    List<Reservation> findByClientIdOrderByDateDebutDesc(Long clientId);

    @EntityGraph(attributePaths = {"station", "client"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.station.id = :stationId
              AND (:statut IS NULL OR r.statut = :statut)
            ORDER BY r.dateDebut DESC
            """)
    List<Reservation> findByStation(@Param("stationId") Long stationId, @Param("statut") StatutReservation statut);

    /** Recherche paginée pour l'administration. */
    @EntityGraph(attributePaths = {"station", "client"})
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE (:statut IS NULL OR r.statut = :statut)
              AND (:stationId IS NULL OR r.station.id = :stationId)
              AND (:q IS NULL
                   OR LOWER(r.client.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(r.client.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(r.client.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(r.station.nom) LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reservation r
            WHERE (:statut IS NULL OR r.statut = :statut)
              AND (:stationId IS NULL OR r.station.id = :stationId)
              AND (:q IS NULL
                   OR LOWER(r.client.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(r.client.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(r.client.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(r.station.nom) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Reservation> search(@Param("statut") StatutReservation statut,
                             @Param("stationId") Long stationId,
                             @Param("q") String q,
                             Pageable pageable);

    boolean existsByClientIdAndStationIdAndStatut(Long clientId, Long stationId, StatutReservation statut);

    long countByStatut(StatutReservation statut);
}
