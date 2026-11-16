package com.washmycar.repository;

import com.washmycar.entity.Station;
import com.washmycar.entity.StatutInscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByUserId(Long userId);

    long countByStatutInscription(StatutInscription statut);

    /** Verrou pessimiste : sérialise les réservations concurrentes sur une même station. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Station s WHERE s.id = :id")
    Optional<Station> findByIdForUpdate(@Param("id") Long id);

    /** Recherche paginée pour l'administration (statut optionnel, texte libre optionnel). */
    @Query(value = """
            SELECT s FROM Station s JOIN FETCH s.user u
            WHERE (:statut IS NULL OR s.statutInscription = :statut)
              AND (:q IS NULL
                   OR LOWER(s.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.ville) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.adresse) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            countQuery = """
            SELECT COUNT(s) FROM Station s JOIN s.user u
            WHERE (:statut IS NULL OR s.statutInscription = :statut)
              AND (:q IS NULL
                   OR LOWER(s.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.ville) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.adresse) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Station> search(@Param("statut") StatutInscription statut, @Param("q") String q, Pageable pageable);

    /**
     * Stations validées dans un rayon donné, triées par distance (formule de Haversine).
     * La pré-sélection sur une « boîte » lat/lng exploite l'index (latitude, longitude)
     * avant le calcul trigonométrique exact.
     */
    @Query(nativeQuery = true, value = """
            SELECT t.id AS id, t.distance AS distance FROM (
                SELECT s.id,
                       2 * 6371 * ASIN(SQRT(
                           POWER(SIN(RADIANS(s.latitude - :lat) / 2), 2)
                         + COS(RADIANS(:lat)) * COS(RADIANS(s.latitude))
                         * POWER(SIN(RADIANS(s.longitude - :lng) / 2), 2)
                       )) AS distance
                FROM stations s
                WHERE s.statut_inscription = 'VALIDEE'
                  AND s.latitude  BETWEEN :minLat AND :maxLat
                  AND s.longitude BETWEEN :minLng AND :maxLng
            ) t
            WHERE t.distance <= :rayonKm
            ORDER BY t.distance
            LIMIT :limite
            """)
    List<StationDistance> findProches(@Param("lat") double lat, @Param("lng") double lng,
                                      @Param("rayonKm") double rayonKm,
                                      @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                      @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                      @Param("limite") int limite);

    /** Projection du résultat de la recherche géographique. */
    interface StationDistance {
        Long getId();
        Double getDistance();
    }
}
