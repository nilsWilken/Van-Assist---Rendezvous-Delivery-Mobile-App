package de.dpd.vanassist.config

import de.dpd.vanassist.R
import de.dpd.vanassist.util.language.CountryItem
import java.util.*

class LocaleConfig {

    companion object {

        /* Custom Locales */
        private val GERMANY_LOCALE = Locale("de","DE")
        private val AMERICA_LOCALE = Locale("en","US")
        private val ROMANIA_LOCALE = Locale("ro","RO")

        /* Language Codes */
        val GERMANY = CountryItem(R.drawable.de, GERMANY_LOCALE)
        val AMERICA = CountryItem(R.drawable.us, AMERICA_LOCALE)
        val ROMANIA = CountryItem(R.drawable.ro, ROMANIA_LOCALE)
    }

}