package com.moneykit.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import timber.log.Timber
import androidx.core.content.edit
import com.moneykit.connect.MkConfiguration
import com.moneykit.connect.MkLinkHandler
import com.moneykit.connect.entities.MkLinkSuccessType

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

        // Initialize Timber for debug logging
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.i("onCreate called with intent: ${intent?.data}")
        
        if (handleOauthRedirectIntent(intent)) {
            // If the app has been launched with an intent which is resuming an oauth flow,
            // it should be handled with linkHandler.continueFlow as shown in
            // handleOauthRedirectIntent(intent)
            return
        }

        // todo Create a link session via the MoneyKit API and pass it to your Android app here
        val linkSessionToken = ""
        
        // Persist your link session token
        this.linkSessionToken = linkSessionToken

        // Initialize the link handler
        val handler = initialiseLinkHandler(linkSessionToken)
        Timber.i("About to present link flow")
        
        // Open the MoneyKit UI to begin the link flow. You must pass Activity context
        // here so that the UI can start.
        handler.presentLinkFlow(this)
        Timber.i("presentLinkFlow called successfully")
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
        Timber.i("Initializing link handler with token: ${linkSessionToken.take(50)}...")
        
        val configuration = MkConfiguration(
            sessionToken = linkSessionToken,
            onSuccess = { successType ->
                Timber.i("onSuccess called with successType: $successType")
                when (successType) {
                    is MkLinkSuccessType.Linked -> {
                        Timber.i("Linked - Id: ${successType.institution.linkId}; Token to exchange: ${successType.institution.token.value}")
                        // Clear the stored token now that the link is complete
                        this.linkSessionToken = null
                        // NOTE: Stopping here as requested - not doing token exchange
                    }

                    is MkLinkSuccessType.Relinked -> {
                        Timber.i("Relinked - Id: ${successType.institution.linkId}")
                    }
                }
            },
            onExit = { error ->
                Timber.i("onExit called with error: $error")
                if (error != null) {
                    Timber.e("Exit error: ${error.displayedMessage}")
                } else {
                    Timber.i("MoneyKit exited without error")
                }
            },
            onEvent = { event ->
                Timber.i("Event received: ${event.name}")
            },
        )

        return MkLinkHandler(configuration).also { linkHandler = it }
    }

    private fun handleOauthRedirectIntent(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        Timber.i("Handling OAuth redirect URI: $uri")

        // If your app started a Bank Oauth flow and is still running in the background after
        // returning from the Bank, the linkHandler instance will still exist in memory and you
        // can use it here. Otherwise the linkHandler should be re-initialised with the same
        // settings and link session token as before.
        val linkHandler = linkHandler
            ?: linkSessionToken?.let { initialiseLinkHandler(it) }
            ?: return false

        Timber.i("About to call continueFlow with URI: $uri")
        linkHandler.continueFlow(this, uri)
        Timber.i("continueFlow called successfully")
        return true
    }
}