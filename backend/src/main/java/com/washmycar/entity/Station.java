package com.washmycar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Station de lavage, rattachée à un compte de rôle STATION. */
@Entity
@Table(name = "stations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 255)
    private String adresse;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 20)
    private String telephone;

    @Column(length = 1000)
    private String description;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "statut_inscription", nullable = false, length = 20)
    private StatutInscription statutInscription;

    /** Moyenne des notes, recalculée à chaque évaluation (0.0 si aucune). */
    @Column(name = "note_moyenne", nullable = false, precision = 2, scale = 1)
    private BigDecimal noteMoyenne;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Horaire> horaires = new ArrayList<>();

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tarif> tarifs = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (statutInscription == null) statutInscription = StatutInscription.EN_ATTENTE;
        if (noteMoyenne == null) noteMoyenne = BigDecimal.ZERO.setScale(1);
    }
}
