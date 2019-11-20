package de.dpd.vanassist.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.dpd.vanassist.R
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.fragment.auth.LoginFragment


@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {


    companion object {

        /* Starts the LoginActivity
        * -> Can be called from any other activity/fragment */
        fun start(activity:AppCompatActivity) {
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    override fun onRestart() {
        super.onRestart()
        /* Initial Creation of the local Database */
        AppDatabase.createInstance(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        /* Initial Creation of the local Database */
        AppDatabase.createInstance(this)

        val actionBar = supportActionBar
        actionBar?.hide()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        startLogInFragment()
    }

    /* Starts the LoginFragment */
    private fun startLogInFragment() {
        val logInFragment = LoginFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.activity_login, logInFragment, FragmentTag.MAP)
            .commitAllowingStateLoss()
    }
}