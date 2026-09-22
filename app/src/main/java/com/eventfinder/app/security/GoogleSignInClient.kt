package com.eventfinder.app.security

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.eventfinder.app.BuildConfig
import com.eventfinder.app.utils.AppLogger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Google single sign-on (FR-01), implemented with AndroidX Credential Manager —
 * the API Google currently recommends for Android sign-in.
 *
 * The OAuth web client ID comes from the `GOOGLE_WEB_CLIENT_ID` gradle property.
 * When it is blank the caller gets [GoogleSignInResult.NotConfigured] so the UI
 * can explain the situation rather than crashing.
 *
 * Reference: https://developer.android.com/identity/sign-in/credential-manager-siwg
 */
class GoogleSignInClient(private val context: Context) {

    private val tag = "GoogleSignInClient"

    /** True when a web client ID has been configured for this build. */
    val isConfigured: Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * Shows the Google account chooser and returns the chosen account.
     *
     * [activityContext] must be an Activity context — Credential Manager needs
     * one to present its UI.
     */
    suspend fun signIn(activityContext: Context): GoogleSignInResult {
        if (!isConfigured) {
            AppLogger.w(tag, "Google sign-in attempted without a configured web client ID")
            return GoogleSignInResult.NotConfigured
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            // false = show every Google account on the device, not only those
            // that have already been used with this app.
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = CredentialManager.create(context)
                .getCredential(activityContext, request)

            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                AppLogger.w(tag, "Unexpected credential type: ${credential.type}")
                return GoogleSignInResult.Failed
            }

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val email = googleCredential.id

            if (email.isBlank()) {
                AppLogger.w(tag, "Google credential carried no account id")
                return GoogleSignInResult.Failed
            }

            AppLogger.i(tag, "Google sign-in succeeded")
            GoogleSignInResult.Success(
                email = email,
                displayName = googleCredential.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore('@')
            )
        } catch (e: GetCredentialCancellationException) {
            AppLogger.i(tag, "Google sign-in cancelled by the user")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            AppLogger.w(tag, "No Google account available on this device: ${e.message}")
            GoogleSignInResult.NoAccount
        } catch (e: GetCredentialException) {
            AppLogger.e(tag, "Google sign-in failed", e)
            GoogleSignInResult.Failed
        }
    }
}

/** Outcome of a Google sign-in attempt. */
sealed interface GoogleSignInResult {
    data class Success(val email: String, val displayName: String) : GoogleSignInResult

    /** The user dismissed the account chooser. */
    data object Cancelled : GoogleSignInResult

    /** The device has no Google account added. */
    data object NoAccount : GoogleSignInResult

    /** No web client ID was set at build time. */
    data object NotConfigured : GoogleSignInResult

    data object Failed : GoogleSignInResult
}
