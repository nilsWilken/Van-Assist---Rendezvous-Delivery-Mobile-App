package de.dpd.vanassist.fragment.main

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.database.repository.CourierRepository
import kotlinx.android.synthetic.main.fragment_intelligence.view.*
import android.content.*
import androidx.appcompat.app.AlertDialog
import de.dpd.vanassist.R
import de.dpd.vanassist.config.ColorConfig
import de.dpd.vanassist.intelligence.timeDependentDarkMode.TimeIntentReceiver
import de.dpd.vanassist.util.FragmentRepo


class IntelligenceFragment : Fragment() {

    private lateinit var api: VanAssistAPIController
    var timeDependentDarkmode: TimeIntentReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_intelligence, container, false)

        api = VanAssistAPIController(activity!! as AppCompatActivity)
        timeDependentDarkmode = TimeIntentReceiver(FragmentRepo.mapActivity as AppCompatActivity)

        val courier = CourierRepository.shared.getCourier()!!
        v.intelligenceModeSwitch.isChecked = courier.ambientIntelligenceMode
        v.timeBasedDarkModeSwitch.isChecked = courier.timeBasedDarkMode
        v.sizeDependentWaitingSwitch.isChecked = courier.sizeDependentWaitingMode
        v.intelligentDrivingSwitch.isChecked = courier.intelligentDrivingMode
        v.dynamicContentSwitch.isChecked = courier.dynamicContentMode
        v.gamificationSwitch.isChecked = courier.gamificationMode

        handleSwitchState(v)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.ambient_intelligence_menu_title)

        v.intelligenceModeButton.setOnClickListener {
            v.intelligenceModeContainer.intelligenceModeSwitch.isChecked =
                !v.intelligenceModeContainer.intelligenceModeSwitch.isChecked
            if (v.intelligenceModeContainer.intelligenceModeSwitch.isChecked) {
                api.enableAmbientIntelligenceMode()
            } else {
                api.disableAmbientIntelligenceMode()
            }

            handleSwitchState(v)
        }

        v.timeBasedDarkModeButton.setOnClickListener {
            if (v.intelligenceModeSwitch.isChecked) {
                v.timeBasedDarkModeContainer.timeBasedDarkModeSwitch.isChecked =
                    !v.timeBasedDarkModeContainer.timeBasedDarkModeSwitch.isChecked
                if (v.timeBasedDarkModeSwitch.isChecked) {
                    api.enableTimeBasedDarkMode()
                    timeDependentDarkmode!!.cancelAlarmService()
                    val themeSwap =  timeDependentDarkmode!!.setDarkModeTimes()
                    if (themeSwap){
                        val builder1 = AlertDialog.Builder(context!!)
                        builder1.setTitle(getString(R.string.ai_timeenableddarkmodeTitle))
                        builder1.setMessage(getString(R.string.ai_timeenableddarkmode_message))
                        builder1.setCancelable(true)

                        builder1.setPositiveButton(
                            getString(R.string.yes),
                            DialogInterface.OnClickListener { dialog, id ->
                                dialog.cancel()
                                if(courier.darkMode){
                                    api.disableDarkMode()
                                } else{
                                    api.enableDarkMode()
                                }
                            })

                        builder1.setNegativeButton(
                            getString(R.string.no), { dialog, id ->
                                /* do nothing */
                                dialog.cancel()
                            })

                        val alert = builder1.create()
                        alert.show()
                    }
                } else {
                    timeDependentDarkmode!!.cancelAlarmService()
                    api.disableTimeBasedDarkMode()

                }
            }
        }

        v.intelligentDrivingButton.setOnClickListener {
            if (v.intelligenceModeSwitch.isChecked) {
                v.intelligentDrivingContainer.intelligentDrivingSwitch.isChecked =
                    !v.intelligentDrivingContainer.intelligentDrivingSwitch.isChecked
                if (v.intelligentDrivingSwitch.isChecked) {
                    api.enableIntelligentDrivingMode()
                } else {
                    api.disableIntelligentDrivingMode()
                }
                handleSwitchState(v)
            }
        }

        v.sizeDependentWaitingButton.setOnClickListener {
            if (v.intelligenceModeSwitch.isChecked && v.intelligentDrivingSwitch.isChecked) {
                v.sizeDependentWaitingContainer.sizeDependentWaitingSwitch.isChecked =
                    !v.sizeDependentWaitingContainer.sizeDependentWaitingSwitch.isChecked
                if (v.sizeDependentWaitingSwitch.isChecked) {
                    api.enableSizeDependentWaitingMode()
                } else {
                    api.disableSizeDependentWaitingMode()
                }
            }
        }

        v.gamificationButton.setOnClickListener {
            if (v.intelligenceModeSwitch.isChecked) {
                v.gamificationContainer.gamificationSwitch.isChecked =
                    !v.gamificationContainer.gamificationSwitch.isChecked
                if (v.gamificationSwitch.isChecked) {
                    api.enableGamificationMode()
                } else {
                    api.disableGamificationMode()
                }
            }
        }

        v.dynamicContentButton.setOnClickListener {
            if (v.intelligenceModeSwitch.isChecked) {
                v.dynamicContentContainer.dynamicContentSwitch.isChecked =
                    !v.dynamicContentContainer.dynamicContentSwitch.isChecked
                if (v.dynamicContentSwitch.isChecked) {
                    api.enableDynamicContentMode()
                } else {
                    api.disableDynamicContentMode()
                }
            }
        }

        v.goto_launchpad_from_intelligence_menu.setOnClickListener { view ->
            activity?.onBackPressed()
        }

        if (courier.darkMode){
            v.timeBasedDarkModeButton.setTextColor(Color.WHITE)
            v.intelligentDrivingButton.setTextColor(Color.WHITE)
            v.sizeDependentWaitingButton.setTextColor(Color.WHITE)
            v.gamificationButton.setTextColor(Color.WHITE)
            v.dynamicContentButton.setTextColor(Color.WHITE)
        } else{
            v.timeBasedDarkModeButton.setTextColor(Color.BLACK)
            v.intelligentDrivingButton.setTextColor(Color.BLACK)
            v.sizeDependentWaitingButton.setTextColor(Color.BLACK)
            v.gamificationButton.setTextColor(Color.BLACK)
            v.dynamicContentButton.setTextColor(Color.BLACK)
        }

        return v;
    }


    private fun handleSwitchState(v: View) {
        if (v.intelligenceModeSwitch.isChecked) {

            v.timeBasedDarkModeSwitch.isEnabled = true
            v.timeBasedDarkModeButton.setTextColor(Color.parseColor(ColorConfig.BLACK))

            v.intelligentDrivingSwitch.isEnabled = true
            v.intelligentDrivingButton.setTextColor(Color.parseColor(ColorConfig.BLACK))

            if(v.intelligentDrivingSwitch.isChecked) {
                v.sizeDependentWaitingSwitch.isEnabled = true
                v.sizeDependentWaitingButton.setTextColor(Color.parseColor(ColorConfig.BLACK))
            } else {
                v.sizeDependentWaitingSwitch.isEnabled = false
                v.sizeDependentWaitingButton.setTextColor(Color.parseColor(ColorConfig.GREY_DISABLED_AI_MODE))
            }

            v.dynamicContentSwitch.isEnabled = true
            v.dynamicContentButton.setTextColor(Color.parseColor(ColorConfig.BLACK))

            v.gamificationSwitch.isEnabled = true
            v.gamificationButton.setTextColor(Color.parseColor(ColorConfig.BLACK))

        } else {

            v.timeBasedDarkModeSwitch.isEnabled = false
            v.timeBasedDarkModeButton.setTextColor(Color.parseColor(ColorConfig.GREY_DISABLED_AI_MODE))

            v.intelligentDrivingSwitch.isEnabled = false
            v.intelligentDrivingButton.setTextColor(Color.parseColor(ColorConfig.GREY_DISABLED_AI_MODE))

            v.sizeDependentWaitingSwitch.isEnabled = false
            v.sizeDependentWaitingButton.setTextColor(Color.parseColor(ColorConfig.GREY_DISABLED_AI_MODE))

            v.dynamicContentSwitch.isEnabled = false
            v.dynamicContentButton.setTextColor(Color.parseColor(ColorConfig.GREY_DISABLED_AI_MODE))

            v.gamificationSwitch.isEnabled = false
            v.gamificationButton.setTextColor(Color.parseColor(ColorConfig.GREY_DISABLED_AI_MODE))
        }

        val courier = CourierRepository.shared.getCourier()!!

        if (courier.darkMode){
            v.intelligenceModeButton.setTextColor(Color.BLACK)
            v.timeBasedDarkModeButton.setTextColor(Color.WHITE)
            v.intelligentDrivingButton.setTextColor(Color.WHITE)
            v.sizeDependentWaitingButton.setTextColor(Color.WHITE)
            v.gamificationButton.setTextColor(Color.WHITE)
            v.dynamicContentButton.setTextColor(Color.WHITE)
        } else{
            v.timeBasedDarkModeButton.setTextColor(Color.BLACK)
            v.intelligentDrivingButton.setTextColor(Color.BLACK)
            v.sizeDependentWaitingButton.setTextColor(Color.BLACK)
            v.gamificationButton.setTextColor(Color.BLACK)
            v.dynamicContentButton.setTextColor(Color.BLACK)
        }
    }


}