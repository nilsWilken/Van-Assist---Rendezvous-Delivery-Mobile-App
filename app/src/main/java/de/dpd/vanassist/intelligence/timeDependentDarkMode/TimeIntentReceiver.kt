package de.dpd.vanassist.intelligence.timeDependentDarkMode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.util.FragmentRepo
import org.shredzone.commons.suncalc.SunTimes
import java.util.*
import de.dpd.vanassist.database.entity.CourierEntity

class TimeIntentReceiver constructor(act: AppCompatActivity) {

    private var alarmIntentMorning: PendingIntent? = null
    private var alarmIntentEvening: PendingIntent? = null
    private var alarmIntent: PendingIntent? = null
    private var brMorning: BroadcastReceiver? = null
    private var brEvening: BroadcastReceiver? = null
    private var am: AlarmManager? = null
    private var main = act
    private var riseHrs : Int
    private var riseMinutes : Int
    private var setHrs : Int
    private var setMinutes : Int
    private lateinit var date : Date
    private lateinit var calendarRise : Calendar
    private lateinit var calendarSet : Calendar
    var api : VanAssistAPIController
    var courier : CourierEntity


    init {
        riseHrs = 0
        riseMinutes = 0
        setHrs = 0
        setMinutes = 0
        main = act
        courier = CourierRepository.shared.getCourier()!!
        api = VanAssistAPIController(main, main.applicationContext)

        brMorning = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "darkModeTimeMorning"){
                    /* if already light mode, do nothing
                     * else perform api.disableDarkMode */
                    if (courier.darkMode)
                        api.disableDarkMode()
                }
            }
        }

        brEvening = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "darkModeTimeEvening"){
                    /* if already dark mode, do nothing
                     * else perform api.enableDarkMode */
                    if (!courier.darkMode)
                        api.enableDarkMode()
                }
            }
        }

        alarmIntentMorning = PendingIntent.getBroadcast(
            FragmentRepo.mapActivity, 0, Intent("darkModeTimeMorning"),
            PendingIntent.FLAG_ONE_SHOT
        )

        alarmIntentEvening = PendingIntent.getBroadcast(
            FragmentRepo.mapActivity, 0, Intent("darkModeTimeEvening"),
            PendingIntent.FLAG_ONE_SHOT
        )

        alarmIntent = PendingIntent.getBroadcast(
            main, 0, Intent("darkModeTime"),
            0
        )

        am = FragmentRepo.mapActivity!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }


    fun setDarkModeTimes() : Boolean {

        main.registerReceiver(brMorning, IntentFilter("darkModeTimeMorning"))
        main.registerReceiver(brEvening, IntentFilter("darkModeTimeEvening"))

        /* Sunrise and Sunset calculation */
        date = Date(System.currentTimeMillis())
        val lat = 49.398750
        val lng = 8.672434
        val times = SunTimes.compute()
            /* set a date */
            .on(date)
            /* set a location */
            .at(lat, lng)
            /* get the results */
            .execute();

        /* creates a new calendar instance */
        calendarRise = GregorianCalendar.getInstance()
        /* creates a new calendar instance */
        calendarSet = GregorianCalendar.getInstance()
        /* assigns calendar to given date */
        calendarRise.time = times.rise
        /* assigns calendar to given date */
        calendarSet.time = times.set
        /* gets hour in 24h format */
        riseHrs = calendarRise.get(Calendar.HOUR_OF_DAY)
        /* gets hour in 24h format */
        riseMinutes = calendarRise.get(Calendar.MINUTE)
        /* gets hour in 24h format */
        setHrs = calendarSet.get(Calendar.HOUR_OF_DAY)
        /* gets hour in 24h format */
        setMinutes = calendarSet.get(Calendar.MINUTE)

        val calendarMorning: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, riseHrs)
            set(Calendar.MINUTE, riseMinutes)
        }

        val calendarEvening: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, setHrs)
            set(Calendar.MINUTE, setMinutes)
        }


        val rightNow = Calendar.getInstance()
        /* return the hour in 24 hrs format (ranging from 0-23) */
        val currentHourIn24Format = rightNow.get(Calendar.HOUR_OF_DAY)
        /* return the hour in 24 hrs format (ranging from 0-23) */
        val currentMinuteIn24Format = rightNow.get(Calendar.MINUTE)

        var alarmSet = "UNDEFINED"
        if (riseHrs > currentHourIn24Format || (riseHrs == currentHourIn24Format && riseMinutes >= currentMinuteIn24Format)) {
            am?.setAlarmClock(AlarmManager.AlarmClockInfo(calendarMorning.timeInMillis, alarmIntentMorning), alarmIntentMorning)
            alarmSet = "MORNING"
        }

        if (setHrs > currentHourIn24Format || (setHrs == currentHourIn24Format && setMinutes >= currentMinuteIn24Format)) {
            am?.setAlarmClock(AlarmManager.AlarmClockInfo(calendarEvening.timeInMillis, alarmIntentEvening), alarmIntentEvening)
            alarmSet = "EVENING"
        }

        var boolThemeSwap = false
        when(alarmSet){
            "MORNING" -> {
                /* Theme must be dark => switch to light comes up */
                if (!courier.darkMode)
                    boolThemeSwap = true
            }
            "EVENING" -> {
                /* Theme must be light => switch to dark comes up */
                if (courier.darkMode)
                    boolThemeSwap = true
            }
            "UNDEFINED" -> {
                /* It is after sunset, but before the day ends, theme must be dark */
                if (!courier.darkMode)
                    boolThemeSwap = true
            }
        }
        return boolThemeSwap
    }


    /* register or UnRegister your broadcast receiver here */
    fun cancelAlarmService(){
        try {
            am!!.cancel(alarmIntentMorning)
            am!!.cancel(alarmIntentEvening)
            main.unregisterReceiver(brMorning)
            main.unregisterReceiver(brEvening)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

}