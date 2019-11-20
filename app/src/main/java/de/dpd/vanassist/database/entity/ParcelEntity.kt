package de.dpd.vanassist.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Parcel")
data class ParcelEntity(

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