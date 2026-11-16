package com.washmycar.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatage et libellés en français. */
object Format {

    /**
     * Les dates de l'API sont en heure de Tunisie : toute comparaison avec « maintenant »
     * doit utiliser ce fuseau, quel que soit le réglage du téléphone.
     */
    private val TUNIS: ZoneId = ZoneId.of("Africa/Tunis")

    fun now(): LocalDateTime = LocalDateTime.now(TUNIS)

    fun today(): LocalDate = LocalDate.now(TUNIS)

    private val FR = Locale.FRANCE
    private val tndFormat = DecimalFormat("#,##0.000", DecimalFormatSymbols(FR))
    private val dateTime = DateTimeFormatter.ofPattern("EEE d MMM yyyy 'à' HH:mm", FR)
    private val date = DateTimeFormatter.ofPattern("d MMM yyyy", FR)
    private val dayShort = DateTimeFormatter.ofPattern("EEE d MMM", FR)
    private val time = DateTimeFormatter.ofPattern("HH:mm", FR)

    /** 12.5 -> « 12,500 TND » (1 dinar = 1000 millimes). */
    fun tnd(value: Double?): String = if (value == null) "—" else "${tndFormat.format(value)} TND"

    fun km(value: Double): String = if (value < 1) "${(value * 1000).toInt()} m" else "%.1f km".format(FR, value)

    fun parse(iso: String): LocalDateTime = LocalDateTime.parse(iso)

    fun dateTime(iso: String): String = parse(iso).format(dateTime).replaceFirstChar { it.uppercase() }

    fun date(iso: String): String = parse(iso).format(date)

    fun dayShort(d: LocalDate): String = d.format(dayShort).replaceFirstChar { it.uppercase() }

    fun time(iso: String): String = parse(iso).format(time)

    fun note(value: Double): String = "%.1f".format(FR, value)

    /** « 08:00:00 » ou « 08:00 » -> « 08:00 ». */
    fun hour(value: String?): String = value?.take(5) ?: "—"

    val STATUT_RESERVATION = mapOf(
        "EN_ATTENTE" to "En attente",
        "ACCEPTEE" to "Acceptée",
        "REFUSEE" to "Refusée",
        "ANNULEE" to "Annulée",
    )

    val STATUT_INSCRIPTION = mapOf(
        "EN_ATTENTE" to "En attente de validation",
        "VALIDEE" to "Validée",
        "REFUSEE" to "Refusée",
    )

    val TYPES_VEHICULE = linkedMapOf(
        "CITADINE" to "Citadine",
        "BERLINE" to "Berline",
        "SUV" to "SUV",
        "UTILITAIRE" to "Utilitaire",
        "MOTO" to "Moto",
    )

    val JOURS = linkedMapOf(
        "LUNDI" to "Lundi",
        "MARDI" to "Mardi",
        "MERCREDI" to "Mercredi",
        "JEUDI" to "Jeudi",
        "VENDREDI" to "Vendredi",
        "SAMEDI" to "Samedi",
        "DIMANCHE" to "Dimanche",
    )

    fun jourOf(d: LocalDate): String = JOURS.keys.elementAt(d.dayOfWeek.value - 1)

    fun vehicule(code: String) = TYPES_VEHICULE[code] ?: code
}
