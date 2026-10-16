package com.emanuel5014.trainable.domain.csvimport

/**
 * The header lines of the workout history CSV, in the language of the app. The export writes them and the import
 * ([HistoryCsvColumns]) reads them back, so they live in one place.
 */
object HistoryCsvHeaders {
    fun basic(languageCode: String, weightUnit: String): String = when (languageCode) {
        "it" -> "Data,ID Sessione,Scheda,Esercizio,Categoria,Tipo,Serie,Peso ($weightUnit),Ripetizioni,Durata (s),Distanza (km),Nota"
        "es" -> "Fecha,ID Sesión,Plan,Ejercicio,Categoría,Tipo,Serie,Peso ($weightUnit),Repeticiones,Duración (s),Distancia (km),Nota"
        "fr" -> "Date,ID Séance,Programme,Exercice,Catégorie,Type,Série,Poids ($weightUnit),Répétitions,Durée (s),Distance (km),Note"
        "de" -> "Datum,Sitzungs-ID,Plan,Übung,Kategorie,Typ,Satz,Gewicht ($weightUnit),Wiederholungen,Dauer (s),Distanz (km),Notiz"
        "pt" -> "Data,ID Sessão,Plano,Exercício,Categoria,Tipo,Série,Peso ($weightUnit),Repetições,Duração (s),Distância (km),Nota"
        else -> "Date,Session ID,Plan,Exercise,Category,Type,Set,Weight ($weightUnit),Reps,Duration (s),Distance (km),Note"
    }

    /** Only written with advanced programming on. */
    fun prescription(languageCode: String): String = when (languageCode) {
        "it" -> "Settimana,% Massimale,Rep Target,Tecniche,RPE,Extra,Riscaldamento"
        "es" -> "Semana,% 1RM,Reps Objetivo,Técnicas,RPE,Extra,Calentamiento"
        "fr" -> "Semaine,% 1RM,Reps Cible,Techniques,RPE,Bonus,Échauffement"
        "de" -> "Woche,% 1RM,Ziel-Wdh.,Techniken,RPE,Extra,Aufwärmen"
        "pt" -> "Semana,% 1RM,Reps Alvo,Técnicas,RPE,Extra,Aquecimento"
        else -> "Week,% 1RM,Target Reps,Techniques,RPE,Extra,Warm-up"
    }

    fun performance(languageCode: String): String = when (languageCode) {
        "it" -> "Mini-serie,Pause (s),Aiutate,Parziali,Tecnica Eseguita"
        "es" -> "Mini-series,Pausas (s),Asistidas,Parciales,Técnica Realizada"
        "fr" -> "Mini-séries,Pauses (s),Aidées,Partielles,Technique Réalisée"
        "de" -> "Minisätze,Pausen (s),Mit Hilfe,Teilwdh.,Ausgeführte Technik"
        "pt" -> "Mini-séries,Pausas (s),Assistidas,Parciais,Técnica Realizada"
        else -> "Mini-sets,Pauses (s),Assisted,Partials,Technique Done"
    }

    /** Whether the prescription of the exercise was changed in that workout (workout / plan), and what it was before. */
    fun programEdit(languageCode: String): String = when (languageCode) {
        "it" -> "Programma Modificato,Programma Originale"
        "es" -> "Programación Modificada,Programación Original"
        "fr" -> "Programmation Modifiée,Programmation d'Origine"
        "de" -> "Programm Geändert,Ursprüngliches Programm"
        "pt" -> "Programação Alterada,Programação Original"
        else -> "Program Changed,Original Program"
    }

    /** How long the whole workout took, in minutes: written on every row of the workout, empty when it is not known. */
    fun sessionDuration(languageCode: String): String = when (languageCode) {
        "it" -> "Durata Allenamento (min)"
        "es" -> "Duración Sesión (min)"
        "fr" -> "Durée Séance (min)"
        "de" -> "Sitzungsdauer (min)"
        "pt" -> "Duração Sessão (min)"
        else -> "Session Duration (min)"
    }

    /** The header line the export writes. */
    fun full(languageCode: String, weightUnit: String, advanced: Boolean): String =
        (if (advanced) basic(languageCode, weightUnit) + "," + prescription(languageCode) else basic(languageCode, weightUnit)) +
            "," + performance(languageCode) + "," + programEdit(languageCode) + "," + sessionDuration(languageCode)
}
