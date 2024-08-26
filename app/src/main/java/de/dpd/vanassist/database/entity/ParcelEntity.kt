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

    val floor: Double,

    val city: String,

    val address: String,

    val additionalAddressInformation: String? = null,

    var deliveryPosition: Int,

    val weight: Double,

    val width: Double,

    val height: Double,

    val length: Double,

    val latitude: String,

    val longitude: String,

    var verificationToken: String,

    val parkingArea: String
)