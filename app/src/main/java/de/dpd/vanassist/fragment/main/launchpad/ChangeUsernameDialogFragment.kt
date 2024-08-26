package de.dpd.vanassist.fragment.main.launchpad

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import de.dpd.vanassist.R

/**
 * Fragment that represents the Dialog Box for changing username
 */
class ChangeUsernameDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;

            builder
                .setTitle(getString(R.string.enter_username_message))
                .setView(inflater.inflate(R.layout.fragment_dialog_change_username, null))
                .setPositiveButton(getString(R.string.ok),
                    DialogInterface.OnClickListener { dialog, id ->
                        // save new username
                    })
                .setNegativeButton(getString(R.string.cancel),
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}