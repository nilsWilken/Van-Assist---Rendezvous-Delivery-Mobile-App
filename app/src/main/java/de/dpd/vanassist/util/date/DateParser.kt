package de.dpd.vanassist.util.date

import java.util.*

class DateParser {

    companion object {
        fun getSecondsSinceMidnight():Long {
            val c = Calendar.getInstance()
            val now = c.timeInMillis
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val passed = now - c.timeInMillis
            val secondsPassed = passed / 1000
            return secondsPassed
        }
    }
}