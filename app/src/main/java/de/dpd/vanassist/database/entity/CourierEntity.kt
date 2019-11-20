package de.dpd.vanassist.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Courier")
data class CourierEntity (

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

    @ColumnInfo(name = "help_mode")
    var helpMode: Boolean,

    @ColumnInfo(name = "ambient_intelligence_mode")
    var ambientIntelligenceMode: Boolean,

    @ColumnInfo(name = "intelligent_driving_mode")
    var intelligentDrivingMode: Boolean,

    @ColumnInfo(name = "size_dependent_waiting_mode")
    var sizeDependentWaitingMode: Boolean,

    @ColumnInfo(name = "time_based_dark_mode")
    var timeBasedDarkMode: Boolean,

    @ColumnInfo(name = "gamification_mode")
    var gamificationMode: Boolean,

    @ColumnInfo(name = "dynamic_content_mode")
    var dynamicContentMode: Boolean,

    @ColumnInfo(name = "language_code")
    var languageCode: String?,

    @ColumnInfo(name = "verification_token")
    val verificationToken: String

)