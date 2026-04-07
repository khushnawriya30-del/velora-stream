package com.cinevault.app.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Launches Google Sign-In via Credential Manager (modern replacement for legacy GoogleSignIn).
 * Returns the Google ID token on success, or throws on failure.
 */
suspend fun getGoogleIdToken(context: Context, webClientId: String): String {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credentialManager = CredentialManager.create(context)
    val result = credentialManager.getCredential(context, request)
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
    return googleIdTokenCredential.idToken
}
