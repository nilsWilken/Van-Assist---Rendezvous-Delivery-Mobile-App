package de.dpd.vanassist.database.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Parcel(

    @PrimaryKey(autoGenerate = false)
    val id: String,

    var state: Int,

    val nameOfRecipient: String,

    val phoneNumber: String?,

    val additionalRecipientInformation: String? = null,

    val address: String,

    val city: String,

    val additionalAddressInformation: String? = null,

    val deliveryPosition: Int,

    val weight: Int,

    val width: Int,

    val height: Int,

    val length: Int,

    val latitude: String,

    val longitude: String,

    var verificationToken: String
)