package de.dpd.vanassist.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Courier")
data class Courier (

    @PrimaryKey(autoGenerate = false)
    val id: String,

    @ColumnInfo(name = "first_name")
    val firstName: String?,

    @ColumnInfo(name = "last_name")
    val lastName: String?,

    @ColumnInfo(name = "user_name")
    var userName: String?,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String?,

    @ColumnInfo(name = "dark_mode")
    var darkMode: Boolean,

    @ColumnInfo(name = "map_label")
    var mapLabel: Boolean,

    @ColumnInfo(name = "language_code")
    var languageCode: String?,

    @ColumnInfo(name = "verification_token")
    val verificationToken: String

)