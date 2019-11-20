package de.dpd.vanassist.intelligence.gamification

import android.content.Context
import de.dpd.vanassist.R
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.util.parcel.ParcelState
import de.dpd.vanassist.util.toast.Toast

class GamificationMode {

    companion object {

        private var threshold= intArrayOf(1,2,3,4,5,10,20,50)


        private fun checkIfThresholdIsReached(parcelLeft:Int):Boolean {
            for(i in threshold) {
                if (i == parcelLeft) {
                    return true
                }
            }
            return false
        }


        fun run(context: Context) {
            val courier = CourierRepository.shared.getCourier()
            if (courier!!.ambientIntelligenceMode) {
                if (courier.gamificationMode) {
                    val parcelList = ParcelRepository.shared.getAll()
                    var counter = 0
                    for (parcel in parcelList) {
                        if (parcel.state == ParcelState.PLANNED) {
                            counter++
                        }
                    }
                    /* Needs to be done
                     * -> This parcel is just moved to (not) delivered after async response
                     * -> This toast is shown before the information is there
                     * -> counter-- just does not count current parcel */
                    counter--
                    if (checkIfThresholdIsReached(counter)) {
                        Toast.createToast(context.getString(R.string.ai_gamification_message_1) + " " + counter + " " + context.getString(R.string.ai_gamification_message_2))
                    }
                }
            }
        }
    }


}