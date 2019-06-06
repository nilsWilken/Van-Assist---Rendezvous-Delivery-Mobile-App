package de.dpd.vanassist.config

import de.dpd.vanassist.R
import de.dpd.vanassist.util.language.CountryItem
import java.util.*

class VanAssistConfig {

    companion object {
        /* General Codes */
        const val MAP_BOX_ACCESS_TOKEN = "pk.eyJ1IjoidmFuYXNzaXN0IiwiYSI6ImNqdTl2eDM0czJjaHY0M2xsYzg1bjFtMmYifQ.YkAR1cSNtoEbI_SSNKcrlg"

        /* Custom Locales */
        val GERMANY_LOCALE = Locale("de","DE")
        val AMERICA_LOCALE = Locale("en","US")
        val ROMANIA_LOCALE = Locale("ro","RO")

        /* Language Codes */
        val GERMANY = CountryItem(R.drawable.de, GERMANY_LOCALE)
        val AMERICA = CountryItem(R.drawable.us, AMERICA_LOCALE)
        val ROMANIA = CountryItem(R.drawable.ro, ROMANIA_LOCALE)

        const val MAP_BOX_LIGHT_STYLE = "mapbox://styles/vanassist/cju9ygw3o1pzi1fml5x9kcbdw"
        const val MAP_BOX_DARK_STYLE = "mapbox://styles/vanassist/cjv68g5pc0o7v1fqqqq86cma8"

        var simulation_running = false
        var dayStarted : Boolean = false




    }
}