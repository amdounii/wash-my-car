package com.washmycar.entity;

import java.time.DayOfWeek;

/** Jours de la semaine (en français, tels que stockés en base). */
public enum JourSemaine {
    LUNDI, MARDI, MERCREDI, JEUDI, VENDREDI, SAMEDI, DIMANCHE;

    /** Convertit un {@link DayOfWeek} Java en jour français. */
    public static JourSemaine from(DayOfWeek day) {
        return values()[day.getValue() - 1];
    }
}
