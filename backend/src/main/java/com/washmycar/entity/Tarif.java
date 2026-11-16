package com.washmycar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Prix d'un service pour un type de véhicule (en dinars tunisiens). */
@Entity
@Table(name = "tarifs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type_vehicule", nullable = false, length = 20)
    private TypeVehicule typeVehicule;

    @Column(nullable = false, length = 100)
    private String service;

    /** Prix en TND (3 décimales = millimes). */
    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal prix;

    /** Durée du lavage : sert à calculer la fin du créneau réservé. */
    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes;
}
