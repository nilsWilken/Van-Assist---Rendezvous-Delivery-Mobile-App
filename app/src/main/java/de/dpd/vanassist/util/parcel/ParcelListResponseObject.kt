package de.dpd.vanassist.util.parcel

import de.dpd.vanassist.database.entity.Parcel
import java.util.ArrayList

class ParcelListResponseObject {

    var parcelList : ArrayList<Parcel>
    var verificationToken : String

    constructor(parcelList: ArrayList<Parcel>, verificationToken: String) {
        this.parcelList = parcelList
        this.verificationToken = verificationToken
    }
}