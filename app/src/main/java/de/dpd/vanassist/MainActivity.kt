package de.dpd.vanassist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.cloud.APIController
import de.dpd.vanassist.cloud.ServiceVolley
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.activity.LoginActivity
import de.dpd.vanassist.activity.MapActivity
import android.view.Menu
import de.dpd.vanassist.database.repository.CourierRepository


class MainActivity : AppCompatActivity() {

    lateinit var cloudButton: Button
    lateinit var txtResult: TextView
    lateinit var authButton: Button
    lateinit var mapButton: Button
    lateinit var loginButton: Button
    val api = VanAssistAPIController(this)
    val service = ServiceVolley()
    val apiController = APIController(service)
    var result = HashMap<String, String?>()
    private lateinit var auth: FirebaseAuth


    override fun onCreateOptionsMenu(menu: Menu): Boolean {


        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)

        return true
    }


    override fun onRestart() {
        super.onRestart()
        var courierRepo = CourierRepository(this)
        val current = courierRepo.getCourier()

        if (current?.darkMode!!) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else{
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Checks if user is logged in --> route to log in screen
        val user =FirebaseAuth.getInstance().currentUser
        if(user == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            finish()
        }


        val actionbar = supportActionBar
        actionbar?.setIcon(R.drawable.vanassist)


        var courierRepo = CourierRepository(this)
        val current = courierRepo.getCourier()

        if (current?.darkMode!!) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else{
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }



        cloudButton = findViewById(R.id.btCloud)

        txtResult = findViewById(R.id.txtResult)
        // Initialize Firebase Auth
       // auth = FirebaseAuth.getInstance()


        cloudButton.setOnClickListener {

            // GET LOCATION TEST : WORKING
            //api.getLocation()

            // GET PARCEL ALL : WORKING
            //api.loadAndSaveAllParcel()

            //PUT SUMMON VAN : WORKING
            //api.putSummonVan("123.0","456.0")

            //POST NEXT PARKING LOCATION Test : WORKING
            //api.postNextParkingLocation("123.0","456.0")

            //GET ALL PARKING LOCATIONS : WORKING
            //api.getAllParkingLocations()

            //GET UID : NOT FOUND 404
            //api.getUID()

            //PUT UPDATE PARCEL ALL : Logic not implemented
            //api.updateAndSaveParcelOrder("list")

            //PUT CONFIRM PARCEL DELIVERY 500 : Internal Server Error
            //api.confirmParcelDeliverySuccess("list")

            //PUT CONFIRM PARCEL DELIVERY FAILURE 500 : Internal Server Error
            //api.confirmParcelDeliveryFailure("List")

            //PUT UNDO PARCEL DELIVERY CONFIRMATION : Logic not implemented
            //api.undoParcelDeliveryConfirmation("String")

            //GET COURIER INFORMATION: unexpected Response Code 400
            //api.getCourierInformation()

            //POST START DELIVERY : 500 Internal Server Error
            //api.postStartDeliveryDay()

            //PUT FINISH DELIVERY DAY : 500 Internal Server Error
            //api.putFinishDeliveryDay()

            //
        }

        mapButton = findViewById(R.id.btnMap)
        mapButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

    }

}
