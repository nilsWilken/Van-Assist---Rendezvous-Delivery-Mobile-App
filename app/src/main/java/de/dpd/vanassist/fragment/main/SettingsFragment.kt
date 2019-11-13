package de.dpd.vanassist.fragment.main

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import de.dpd.vanassist.R
import kotlinx.android.synthetic.main.fragment_settings.view.*
import android.widget.AdapterView
import android.widget.Spinner
import de.dpd.vanassist.adapters.CountryAdapter
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.config.Path
import de.dpd.vanassist.util.language.CountryItem
import java.util.*
import android.content.DialogInterface
import android.content.res.Configuration
import androidx.appcompat.app.AlertDialog
import android.util.Log
import de.dpd.vanassist.util.language.LanguageManager


class SettingsFragment : androidx.fragment.app.Fragment() {

    private lateinit var courierRepo: CourierRepository

    private lateinit var countryList:ArrayList<CountryItem>
    private lateinit var countryAdapter:CountryAdapter
    private var currentLanguage:Int = -1
    private lateinit var countrySpinner:Spinner
    private var check = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_settings, container, false)


        courierRepo = CourierRepository(activity!!)

        val appLocale = Configuration(context!!.getResources().getConfiguration()).locale.toString()
        val currentPos = LanguageManager.getPositionByCountryCode(appLocale)
        this.currentLanguage = currentPos

        this.countryList = LanguageManager.createCountryItemList()


        this.countrySpinner = v.spinner
        countryAdapter = CountryAdapter(context!!, countryList)
        this.countrySpinner.adapter = countryAdapter
        this.countrySpinner.setSelection(currentPos)
        this.countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(++check > 1) {
                    val locale = countryList[position].locale
                    setLocale(locale)
                }
            }
        }

        //setButtonEffect(v.usernameButton)
        setButtonEffect(v.themeButton)
        setButtonEffect(v.helperLabelsButton)
        setButtonEffect(v.privacyButton)
        setButtonEffect(v.logoutButton)
        val courier = courierRepo.getCourier()
        v.usernameButton.text = courier!!.firstName!!.trim() + " " + courier.lastName!!.trim()

        /*v.usernameButton.setOnClickListener {
            val changeUsernameDialog = ChangeUsernameDialogFragment()
            val window = changeUsernameDialog.activity?.window
            window?.setGravity(Gravity.BOTTOM)
            changeUsernameDialog.show(activity?.supportFragmentManager, "changeUser")
        } */

        val themeValue = courierRepo.getCourier()?.darkMode

        if (themeValue != null) {
            v.themeButtonContainer.themeSwitch.isChecked = themeValue
        }

        v.themeButton.setOnClickListener {
            setDarkMode(themeValue!!, v)
        }


        v.helperLabelsButton.setOnClickListener {
            v.helperLabelsButtonContainer.helperLabelsSwitch.isChecked = !v.helperLabelsButtonContainer.helperLabelsSwitch.isChecked
        }
        val labelValue = courierRepo.getCourier()?.mapLabel
        if (labelValue != null) {
            v.helperLabelsButtonContainer.helperLabelsSwitch.isChecked = labelValue
        }
        //handler for helper labels toggling
        v.helperLabelsButtonContainer.helperLabelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val currentUser = courierRepo.getCourier()
            if (isChecked) {
                val api = VanAssistAPIController(activity!! as AppCompatActivity)
                api.enableMapLabel()
            }
            else {
                val api = VanAssistAPIController(activity!! as AppCompatActivity)
                api.disableMapLabel()
            }
        }


        v.privacyButton.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(Path.PRIVACY))
            startActivity(i)
        }


        v.logoutButton.setOnClickListener{
            val confirmDialog = LogoutDialogFragment()
            confirmDialog.show(activity?.supportFragmentManager, "confirm")
        }


        v.goto_launchpad_from_settings.setOnClickListener { view ->
            activity?.onBackPressed()
        }

        return v
    }


    /**
     * method that sets an effect to change background color on click
     * @param button
     */
    private fun setButtonEffect(button: View) {
        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.background.setColorFilter(0x25000000, PorterDuff.Mode.ADD)
                    view.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    view.background.clearColorFilter()
                    view.invalidate()
                }
            }
            false
        }
    }


    fun refreshActivity() {

        activity!!.recreate()
    }



    fun setDarkMode(darkMode: Boolean, v : View){
        val builder1 = AlertDialog.Builder(context!!)
        builder1.setTitle(getString(R.string.dark_mode_alert_title))
        builder1.setMessage(getString(R.string.dark_mode_alert_message))
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            getString(R.string.yes),
            DialogInterface.OnClickListener { dialog, id ->
                dialog.cancel()

                Log.wtf("Theme set: DarkMode = ", "+-> " + darkMode  )

                // do update
                val themeValue = courierRepo.getCourier()?.darkMode

                if (themeValue!!) {
                    //disable it
                    v.themeButtonContainer.themeSwitch.isChecked = false

                    val api = VanAssistAPIController(activity!! as AppCompatActivity)
                    api.disableDarkMode()
                }
                else {
                    v.themeButtonContainer.themeSwitch.isChecked = true
                    val api = VanAssistAPIController(activity!! as AppCompatActivity)
                    api.enableDarkMode()
                }

            })

        builder1.setNegativeButton(
            getString(R.string.no),
            DialogInterface.OnClickListener { dialog, id ->
                //do nothing
                v.themeButtonContainer.themeSwitch.isChecked = darkMode
                dialog.cancel()
            })

        val alert = builder1.create()
        alert.show()

    }

    fun setLocale(locale: Locale) {

        val builder1 = AlertDialog.Builder(context!!)
        builder1.setTitle(getString(R.string.language_alert_title))
        builder1.setMessage(getString(R.string.language_alert_message))
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            getString(R.string.yes),
            DialogInterface.OnClickListener { dialog, id ->
                dialog.cancel()
                var languageCode = locale.toString()
                //change locally
                LanguageManager.setLocale(locale, context!!)
                Log.wtf("LanguageCode","Code-> " + languageCode)
                courierRepo.updateLanguageCode(languageCode)
                Log.wtf("LanguageCode","Code-> " + courierRepo.getCourier()!!.languageCode)

                //change remote (async)
                val api = VanAssistAPIController(activity!! as AppCompatActivity)
                api.changeLanguage(locale.toString())
                refreshActivity()
            })

        builder1.setNegativeButton(
            getString(R.string.no),
            DialogInterface.OnClickListener { dialog, id ->
                this.check = 0
                this.countrySpinner.setSelection(currentLanguage)
                dialog.cancel()
            })

        val alert = builder1.create()
        alert.show()
    }
}