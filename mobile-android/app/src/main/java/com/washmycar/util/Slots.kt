package com.washmycar.util

import com.washmycar.data.model.CreneauOccupe
import com.washmycar.data.model.Horaire
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Calcule les créneaux proposés au client pour un jour donné :
 * dans les horaires d'ouverture, sans chevauchement avec les créneaux déjà occupés,
 * et dans le futur. Le backend revérifie toutes ces règles à la réservation.
 */
object Slots {

    fun compute(
        date: LocalDate,
        horaire: Horaire?,
        dureeMinutes: Int,
        occupes: List<CreneauOccupe>,
        maintenant: LocalDateTime = Format.now(),
        pasMinutes: Int = 30,
    ): List<LocalTime> {
        if (horaire == null || horaire.ferme) return emptyList()
        val ouverture = horaire.heureOuverture?.let { minutesOf(it) } ?: return emptyList()
        val fermeture = horaire.heureFermeture?.let { minutesOf(it) } ?: return emptyList()
        val intervalles = occupes.map { Format.parse(it.debut) to Format.parse(it.fin) }

        // Calcul en minutes depuis minuit pour éviter tout débordement après 23:59.
        return (ouverture..(fermeture - dureeMinutes) step pasMinutes)
            .map { LocalTime.of(it / 60, it % 60) }
            .filter { t ->
                val debut = date.atTime(t)
                val fin = debut.plusMinutes(dureeMinutes.toLong())
                debut.isAfter(maintenant) && intervalles.none { (d, f) -> d < fin && f > debut }
            }
    }

    private fun minutesOf(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }
}
