package com.voxcom.rollerrush.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.voxcom.rollerrush.R

class SettingsActivity : BaseActivity() {

    private lateinit var playerName: TextView
    private lateinit var accountStatus: TextView
    private lateinit var connectionStatus: TextView

    private lateinit var editUsernameButton: Button
    private lateinit var linkGoogleButton: Button
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var hitboxesSwitch: SwitchCompat
    private lateinit var resetProgressButton: Button
    private lateinit var backButton: Button
    private lateinit var deleteAccountButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.voxcom.rollerrush.R.layout.activity_settings)

        bindViews()
        setupSettings()
        setupClickListeners()
        refreshProfileCard()
    }

    private fun bindViews() {
        playerName = findViewById(R.id.playerName)
        accountStatus = findViewById(R.id.accountStatus)
        connectionStatus = findViewById(R.id.connectionStatus)

        editUsernameButton = findViewById(R.id.editUsernameButton)
        linkGoogleButton = findViewById(R.id.linkGoogleButton)
        soundSwitch = findViewById(R.id.soundSwitch)
        hitboxesSwitch = findViewById(R.id.hitboxesSwitch)
        resetProgressButton = findViewById(R.id.resetProgressButton)
        backButton = findViewById(R.id.backButton)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)
    }

    private fun setupSettings() {

        soundSwitch.isChecked = app.prefs.soundEnabled

        soundSwitch.setOnCheckedChangeListener { _, enabled ->
            app.prefs.soundEnabled = enabled
        }

        hitboxesSwitch.isChecked = app.prefs.debugHitboxes

        hitboxesSwitch.setOnCheckedChangeListener { _, enabled ->
            app.prefs.debugHitboxes = enabled
        }
    }

    private fun setupClickListeners() {

        deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Request Account Deletion")
                .setMessage(
                    "You will be taken to the Roller Rush account " +
                            "deletion page, where you can submit a request " +
                            "to delete your account and associated online data."
                )
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CONTINUE") { _, _ ->
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://rollerrush-online.onrender.com/account-deletion.html"
                        )
                    )
                    startActivity(intent)
                }
                .show()
        }

        backButton.setOnClickListener {
            finish()
        }

        editUsernameButton.setOnClickListener {
            showEditUsernameDialog()
        }

        linkGoogleButton.setOnClickListener {
            if (app.firebaseGameService.isGoogleLinked) {
                Toast.makeText(
                    this,
                    "Your Google account is already linked.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Google account linking will be enabled in the upcoming updates.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        resetProgressButton.setOnClickListener {
            showResetConfirmation()
        }
    }

    private fun refreshProfileCard() {

        val service = app.firebaseGameService

        playerName.text = service.displayName ?: "Loading player..."

        accountStatus.text = when {
            service.isGoogleLinked -> "Google account linked"
            service.isAnonymous -> "Guest account"
            service.isLoggedIn -> "Account signed in"
            else -> "Not signed in"
        }

        connectionStatus.text = if (service.isLoggedIn) {
            "Account ready"
        } else {
            "Connecting to server"
        }

        linkGoogleButton.text = if (service.isGoogleLinked) {
            "GOOGLE ACCOUNT LINKED"
        } else {
            "LINK GOOGLE ACCOUNT"
        }

        linkGoogleButton.isEnabled = !service.isGoogleLinked

        // The application may still be completing anonymous sign-in
        // when this screen is opened.
        if (service.displayName.isNullOrBlank()) {

            service.initialize { success, error ->

                runOnUiThread {

                    if (success) {
                        refreshProfileCard()
                    } else {
                        playerName.text = "Player profile unavailable"
                        connectionStatus.text =
                            error ?: "Could not connect to Server"
                    }
                }
            }
        }
    }

    private fun showEditUsernameDialog() {

        val input = EditText(this).apply {
            hint = "3–16 letters, numbers or underscores"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(16))

            setText(app.firebaseGameService.displayName.orEmpty())
            setSelection(text.length)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change username")
            .setMessage("Choose a unique username for Roller Rush.")
            .setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", null)
            .create()

        dialog.setOnShowListener {

            val saveButton = dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            saveButton.setOnClickListener {

                saveButton.isEnabled = false
                input.error = null

                app.firebaseGameService.changeUsername(
                    input.text.toString()
                ) { success, error ->

                    runOnUiThread {

                        saveButton.isEnabled = true

                        if (success) {

                            refreshProfileCard()
                            dialog.dismiss()

                            Toast.makeText(
                                this,
                                "Username updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            input.error = error
                                ?: "Could not update username."
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showResetConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Reset local progress?")
            .setMessage(
                "This will clear your local game progress, " +
                        "coins, unlocked items and saved settings on this device.\n\n" +
                        "Your RollerRush account will not be deleted."
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("RESET") { _, _ ->

                app.playerData.resetAll()

                // Restore the displayed default settings.
                soundSwitch.isChecked = app.prefs.soundEnabled
                hitboxesSwitch.isChecked = app.prefs.debugHitboxes

                Toast.makeText(
                    this,
                    "Local progress has been reset.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        app.bgmManager.playLobby()
    }
}