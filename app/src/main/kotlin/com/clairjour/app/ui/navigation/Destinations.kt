package com.clairjour.app.ui.navigation

import kotlinx.datetime.LocalDate

object Destinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val JOURNAL = "journal"
    const val JOURNAL_EDITOR = "journal/editor?date={date}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SHIELD = "shield"
    const val ADDICTION_EDIT = "addiction/edit?addictionId={addictionId}"

    fun journalEditor(date: LocalDate? = null) =
        if (date == null) "journal/editor" else "journal/editor?date=$date"

    fun addictionEdit(id: String? = null) =
        if (id == null) "addiction/edit" else "addiction/edit?addictionId=$id"
}
