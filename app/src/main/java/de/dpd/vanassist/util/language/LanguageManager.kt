package de.dpd.vanassist.util.language

import android.content.Context
import android.content.res.Configuration
import de.dpd.vanassist.config.LocaleConfig
import java.util.*

class LanguageManager {

    companion object {

        fun createCountryItemList():ArrayList<CountryItem> {
            val countryList = ArrayList<CountryItem>()
            countryList.add(LocaleConfig.GERMANY)
            countryList.add(LocaleConfig.AMERICA)
            countryList.add(LocaleConfig.ROMANIA)
            return countryList
        }


        fun getPositionByCountryCode(countryCode:String):Int {
            var pos = 0
            val countryItemList = createCountryItemList()
            for(countryItem in countryItemList) {
                if(countryItem.locale.toString() == countryCode) {
                    return pos
                }
                pos++
            }
            return 0
        }


        fun setLocale(locale:Locale, context:Context) {
            Locale.setDefault(locale)
            val res = context.resources
            val config = Configuration(res.configuration)
            config.locale = locale
            res.updateConfiguration(config, res.displayMetrics)
        }


        fun createLocale(languageCode:String):Locale {
            val result = languageCode.split("_")
            return Locale(result[0], result[1])
        }
    }
}