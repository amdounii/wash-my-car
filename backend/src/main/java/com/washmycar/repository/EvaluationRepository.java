package com.washmycar.repository;

import com.washmycar.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByClientIdAndStationId(Long clientId, Long stationId);

    @EntityGraph(attributePaths = {"client", "station"})
    List<Evaluation> findByStationIdOrderByDateCreationDesc(Long stationId);

    @EntityGraph(attributePaths = {"client", "station"})
    List<Evaluation> findByClientIdOrderByDateCreationDesc(Long clientId);

    long countByStationId(Long stationId);

    @Query("SELECT AVG(e.note) FROM Evaluation e WHERE e.station.id = :stationId")
    Double averageNote(@Param("stationId") Long stationId);

    /** Recherche paginée pour la modération (station et note optionnelles). */
    @EntityGraph(attributePaths = {"client", "station"})
    @Query(value = """
            SELECT e FROM Evaluation e
            WHERE (:stationId IS NULL OR e.station.id = :stationId)
              AND (:note IS NULL OR e.note = :note)
              AND (:q IS NULL
                   OR LOWER(e.commentaire) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.client.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.station.nom) LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            countQuery = """
            SELECT COUNT(e) FROM Evaluation e
            WHERE (:stationId IS NULL OR e.station.id = :stationId)
              AND (:note IS NULL OR e.note = :note)
              AND (:q IS NULL
                   OR LOWER(e.commentaire) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.client.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.station.nom) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Evaluation> search(@Param("stationId") Long stationId, @Param("note") Integer note,
                            @Param("q") String q, Pageable pageable);
}
