package de.dpd.vanassist.util.parcel

import de.dpd.vanassist.config.ParcelConfig
import de.dpd.vanassist.database.entity.ParcelEntity

class ParcelUtil {

    companion object {

        const val XS = 35
        const val S = 50
        const val M = 70
        const val L = 90

        fun getParcelSize(parcel: ParcelEntity):String {
            val sum = parcel.length + parcel.width + parcel.height
            if(sum <= XS) {
                return ParcelConfig.XS
            } else if(sum <= S) {
                return ParcelConfig.S
            } else if(sum <= M) {
                return ParcelConfig.M
            } else if(sum <= L) {
                return ParcelConfig.L
            }
            return ParcelConfig.XL
        }
    }
}