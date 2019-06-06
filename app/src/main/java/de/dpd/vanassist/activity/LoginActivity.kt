package de.dpd.vanassist.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.dpd.vanassist.R
import de.dpd.vanassist.fragment.auth.LoginFragment


@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {


    companion object {

        fun start(act:AppCompatActivity) {
            val intent = Intent(act, LoginActivity::class.java)
            act.startActivity(intent)
            act.finish()
        }

    }

    override fun onRestart() {
        super.onRestart()

//        var courierRepo = CourierRepository(this)
//        val current = courierRepo.getCourier()
//
//        if (current?.darkMode!!) {
//            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//        }
//        else{
//            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {

        val actionBar = supportActionBar
        actionBar?.hide()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

//        val courierRepo = CourierRepository(this)
//        val current = courierRepo.getCourier()
//
//        if (current != null && current.darkMode) {
//            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//        }
//        else{
//            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }

        startLogInFragment()

    }

    private fun startLogInFragment() {
        val logInFragment = LoginFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.activity_login, logInFragment, "map")
            .commitAllowingStateLoss()
    }
}