package de.dpd.vanassist.fragment.auth

import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.Path
import kotlinx.android.synthetic.main.fragment_login.view.*

class LoginFragment : Fragment() {

    private var api:VanAssistAPIController? = null

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }

    /**
     * Created by Axel Herbsreith and Jasmin Weimüller
     * Handles Login Activity and Authentication on enter keypress
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v =  inflater.inflate(R.layout.fragment_login, container, false)

        this.api = VanAssistAPIController(activity as AppCompatActivity)

        val usernameLayout = v.userNameLayout as TextInputLayout
        usernameLayout.hint = getString(R.string.username)

        val passwordLayout = v.passwordLayout as TextInputLayout
        passwordLayout.hint = getString(R.string.password)

        val userNameField = v.userNameField as EditText
        val passwordField = v.passwordField as EditText
        passwordField.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        passwordField.typeface = Typeface.DEFAULT
        passwordField.transformationMethod = PasswordTransformationMethod()


        val logInButton = v.logInButton as Button
        logInButton.setOnClickListener {

            val username = userNameField.text.toString().trim().toLowerCase()
            val password = passwordField.text.toString().trim()

            this.api!!.userAuthentication(activity as AppCompatActivity, username, password)

        }

        passwordField.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                logInButton.performClick()
                return@OnKeyListener true
            }
            false
        })

        val textviewHyperlink = v.findViewById<TextView>(R.id.impressum)
        textviewHyperlink.text = Html.fromHtml(Path.TERMS_OF_USE)
        textviewHyperlink.movementMethod = LinkMovementMethod.getInstance()

        return v
    }


}
