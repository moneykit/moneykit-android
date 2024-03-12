package com.moneykit.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.edit
import com.moneykit.connect.MkConfiguration
import com.moneykit.connect.MkLinkHandler
import com.moneykit.connect.entities.MkLinkSuccessType
import timber.log.Timber

private const val LINK_SESSION_TOKEN_PREF_KEY = "link_session_token"

class MainActivity : Activity() {
    private var linkHandler: MkLinkHandler? = null

    private val prefs by lazy { getPreferences(Context.MODE_PRIVATE) }

    // Your link session token needs to be persisted in case the app is launched by an oauth flow
    // and needs to resume the existing link session. This is an example of how to persist the
    // token in Android Preferences.
    private var linkSessionToken
        get() = prefs.getString(LINK_SESSION_TOKEN_PREF_KEY, null)
        set(token) = prefs.edit {
            putString(LINK_SESSION_TOKEN_PREF_KEY, token)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (handleOauthRedirectIntent(intent)) {
            // If the app has been launched with an intent which is resuming an oauth flow,
            // it should be handled with linkHandler.continueFlow as shown in
            // handleOauthRedirectIntent(intent)
            return
        }

        // todo Create a link session via the MoneyKit API and pass it to your Android app here
        val linkSessionToken = "YOUR_LINK_SESSION_TOKEN"
        // Persist your link session token
        this.linkSessionToken = linkSessionToken

        // Initialise the link handler
        initialiseLinkHandler(linkSessionToken)
            // Open the MoneyKit UI to begin the link flow. You must pass Activity context
            // here so that the UI can start.
            .presentLinkFlow(this)
    }

    /**
     * If your app is still running when it is given an oauth redirect URL to handle, the redirect
     * URL will be provided here as an intent
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleOauthRedirectIntent(intent)
    }

    private fun initialiseLinkHandler(linkSessionToken: String): MkLinkHandler {
        val configuration = MkConfiguration(
            sessionToken = linkSessionToken,
            onSuccess = { successType ->
                when (successType) {
                    is MkLinkSuccessType.Linked -> {
                        // Handle successful link - pass successType.institution.token to your
                        // server to be exchanged for a link token
                        Timber.i("Successful link")
                    }

                    is MkLinkSuccessType.Relinked -> {
                        // Handle successful relink
                        Timber.i("Successful relink")
                    }
                }
            },
            onExit = { error ->
                if (error != null) {
                    // Optional, log errors for your own tracing
                    Timber.e(error.displayedMessage)
                }

                // Handle MoneyKit being exited, if you need to reset UI etc.
            },
            onEvent = { event ->
                // Optional, log events for your own tracing
                Timber.i(event.name)
            },
        )

        return MkLinkHandler(configuration).also { linkHandler = it }
    }

    private fun handleOauthRedirectIntent(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false

        // If your app started a Bank Oauth flow and is still running in the background after
        // returning from the Bank, the linkHandler instance will still exist in memory and you
        // can use it here. Otherwise the linkHandler should be re-initialised with the same
        // settings and link session token as before.
        val linkHandler = linkHandler
            ?: linkSessionToken?.let { initialiseLinkHandler(it) }
            ?: return false

        linkHandler.continueFlow(this, uri)
        return true
    }
}
