package com.clairjour.app.domain

import android.content.Context
import kotlinx.datetime.LocalDate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object Formatters {
    private val EURO: Currency = Currency.getInstance("EUR")

    fun currency(amount: Double, context: Context): String {
        val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
        // Amounts are entered in euros; an in-app language without a country ("fr") has no currency.
        return NumberFormat.getCurrencyInstance(locale).apply { currency = EURO }.format(amount)
    }

    fun date(date: LocalDate, context: Context): String {
        val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
        val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
        return javaDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
}
