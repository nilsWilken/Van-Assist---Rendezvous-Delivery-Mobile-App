package de.dpd.vanassist.fragment.main.launchpad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.mapbox.geojson.Point
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.MapBoxConfig
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.fragment.main.map.MapFragmentOld
import de.dpd.vanassist.util.FragmentRepo
import kotlinx.android.synthetic.main.fragment_vehicle_problem_details.*
import kotlinx.android.synthetic.main.fragment_vehicle_problem_details.view.*
import kotlinx.android.synthetic.main.fragment_vehicle_status.*

class ProblemStatusDetailsDialogFragment : Fragment() {

    private lateinit var api: VanAssistAPIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_vehicle_problem_details, container, false)

        val vanObserver = Observer<VanEntity> { van ->
            problem_details_van_id_value_text_view.text = van!!.id

            var messageBuilder = StringBuilder(van!!.problemMessage)
            var x = 40
            var index = 0
            while(x < messageBuilder.length) {
                index = messageBuilder.indexOf(" ", x)
                if (index < 0) {
                    break
                }
                messageBuilder.replace(index, index+1, "\n\t")
                x = index + 40
            }
            messageBuilder.insert(0, "\t")

            problem_details_problem_message_value_text_view.text = messageBuilder.toString()

            problem_details_problem_position_value_text_view.text = "%.5f".format(van!!.latitude).replace(",", ".") + "; " + "%.5f".format(van!!.longitude).replace(",", ".")
        }

        VanRepository.shared.getVanFlowById(VanAssistConfig.VAN_ID).observe(viewLifecycleOwner, vanObserver)

       // val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)

        v.goto_vehicle_status_from_vehicle_problem_details_menu.setOnClickListener {
            //activity?.onBackPressed()
            (activity as MapActivity).startLaunchpadFragmentWithBackstack()
        }

        v.problem_details_set_new_parking_position.setOnClickListener {
            //activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.INVISIBLE
            //activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "OK"

            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!, mapActivity.applicationContext)
            apiController.setProblemSolved(0)
            activity?.onBackPressed()
        }

        v.problem_details_button_continue_mission.setOnClickListener {
            //activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.INVISIBLE
            //activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "OK"

            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!, mapActivity.applicationContext)
            apiController.setProblemSolved(1)
            activity?.onBackPressed()
        }

        v.problem_details_show_on_map.setOnClickListener {
            val mapFragment = MapFragmentOld.newInstance()
            mapFragment.setShowVanLocationOnCreation()
            this.requireActivity().supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                ?.addToBackStack(FragmentTag.MAP)
                ?.commit()
            val vanEntity = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)
            val location = Point.fromLngLat(vanEntity!!.longitude, vanEntity!!.latitude)
            //(this.requireActivity().supportFragmentManager.findFragmentByTag(FragmentTag.MAP) as MapFragmentOld).showVanLocation(MapBoxConfig.MAX_ZOOM - 3, true)
            //mapFragment.showVanLocation(MapBoxConfig.MAX_ZOOM - 3, true)
        }


        return v
    }
}