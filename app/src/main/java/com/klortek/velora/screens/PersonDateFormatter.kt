package com.klortek.velora.screens

import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formats Jellyfin person dates using the language selected by the device/app. */
internal fun formatPersonDate(dateString: String, locale: Locale = Locale.getDefault()): String {
    val datePart = dateString.substringBefore('T').trim()
    if (datePart.isEmpty()) return dateString

    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).let { parser ->
        parser.isLenient = false
        val position = ParsePosition(0)
        parser.parse(datePart, position)?.takeIf { position.index == datePart.length }
    }

    return parsed?.let { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(it) }
        ?: datePart
}
