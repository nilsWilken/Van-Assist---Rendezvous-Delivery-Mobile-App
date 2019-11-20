package de.dpd.vanassist.util.parcel

import de.dpd.vanassist.database.entity.ParcelEntity
import java.util.ArrayList

class ParcelListResponseObject {

    var parcelList : ArrayList<ParcelEntity>
    var verificationToken : String

    constructor(parcelList: ArrayList<ParcelEntity>, verificationToken: String) {
        this.parcelList = parcelList
        this.verificationToken = verificationToken
    }
}