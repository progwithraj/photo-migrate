package com.photomigrate.app.data.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.photomigrate.app.data.model.GoogleAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class OAuthManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("photo_migrate_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        const val REDIRECT_URI_SCHEME = "com.googleusercontent.apps.699958644859-gqrjba6g17kualdgr7h1c86stc50r90k"
        const val REDIRECT_URI = "$REDIRECT_URI_SCHEME:/oauthredirect"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo"
        const val ABOUT_ENDPOINT = "https://www.googleapis.com/drive/v3/about?fields=storageQuota,user"

        val SCOPES = listOf(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/photoslibrary.readonly",
            "https://www.googleapis.com/auth/photoslibrary.appendonly",
            "https://www.googleapis.com/auth/drive.readonly",
            "https://www.googleapis.com/auth/drive"
        ).joinToString(" ")
    }

    // Save custom Client ID & Secret set by user (or default)
    fun getClientId(): String {
        val id = prefs.getString("custom_client_id", "") ?: ""
        return id.ifEmpty { "699958644859-gqrjba6g17kualdgr7h1c86stc50r90k.apps.googleusercontent.com" }
    }
    fun getClientSecret(): String = prefs.getString("custom_client_secret", "") ?: ""

    fun saveClientCredentials(clientId: String, clientSecret: String) {
        prefs.edit()
            .putString("custom_client_id", clientId.trim())
            .putString("custom_client_secret", clientSecret.trim())
            .apply()
    }

    fun generateAuthUrl(): String {
        val verifier = generateCodeVerifier()
        prefs.edit().putString("pending_verifier", verifier).apply()
        val challenge = generateCodeChallenge(verifier)

        val clientId = getClientId()

        val uri = Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()

        return uri.toString()
    }

    fun getPendingVerifier(): String = prefs.getString("pending_verifier", "") ?: ""

    fun exchangeCodeForToken(code: String, verifier: String, onResult: (GoogleAccount?, String?) -> Unit) {
        val clientId = getClientId()
        val clientSecret = getClientSecret()

        if (clientId.isEmpty()) {
            onResult(null, "Please configure your free Google Cloud Client ID in Setup Guide first.")
            return
        }

        val bodyBuilder = FormBody.Builder()
            .add("client_id", clientId)
            .add("code", code)
            .add("code_verifier", verifier)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", REDIRECT_URI)

        if (clientSecret.isNotEmpty()) {
            bodyBuilder.add("client_secret", clientSecret)
        }

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(bodyBuilder.build())
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    onResult(null, "Token error (${response.code}): $responseBody")
                    return@Thread
                }

                val json = gson.fromJson(responseBody, Map::class.java)
                val accessToken = json["access_token"] as? String ?: ""
                val refreshToken = json["refresh_token"] as? String ?: ""
                val expiresIn = (json["expires_in"] as? Double)?.toLong() ?: 3600L

                // Fetch User Profile & Storage info
                val account = fetchUserProfileAndQuota(accessToken, refreshToken, System.currentTimeMillis() + (expiresIn * 1000L))
                if (account != null) {
                    saveAccount(account)
                    onResult(account, null)
                } else {
                    onResult(null, "Failed to retrieve user profile.")
                }
            } catch (e: Exception) {
                onResult(null, "Network error: ${e.localizedMessage}")
            }
        }.start()
    }

    suspend fun refreshTokenIfNeededSuspend(account: GoogleAccount): GoogleAccount? = withContext(Dispatchers.IO) {
        if (!account.isTokenExpired()) {
            return@withContext account
        }

        val clientId = getClientId()
        val clientSecret = getClientSecret()
        if (account.refreshToken.isEmpty() || clientId.isEmpty()) {
            return@withContext null
        }

        val bodyBuilder = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", account.refreshToken)

        if (clientSecret.isNotEmpty()) {
            bodyBuilder.add("client_secret", clientSecret)
        }

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(bodyBuilder.build())
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, Map::class.java)
                val newAccessToken = json["access_token"] as? String ?: ""
                val expiresIn = (json["expires_in"] as? Double)?.toLong() ?: 3600L

                val updatedAccount = account.copy(
                    accessToken = newAccessToken,
                    tokenExpirationTimeMillis = System.currentTimeMillis() + (expiresIn * 1000L)
                )
                saveAccount(updatedAccount)
                updatedAccount
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun refreshTokenIfNeeded(account: GoogleAccount, onResult: (GoogleAccount?) -> Unit) {
        if (!account.isTokenExpired()) {
            onResult(account)
            return
        }

        val clientId = getClientId()
        val clientSecret = getClientSecret()
        if (account.refreshToken.isEmpty() || clientId.isEmpty()) {
            onResult(null)
            return
        }

        val bodyBuilder = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", account.refreshToken)

        if (clientSecret.isNotEmpty()) {
            bodyBuilder.add("client_secret", clientSecret)
        }

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(bodyBuilder.build())
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val newAccessToken = json["access_token"] as? String ?: ""
                    val expiresIn = (json["expires_in"] as? Double)?.toLong() ?: 3600L

                    val updatedAccount = account.copy(
                        accessToken = newAccessToken,
                        tokenExpirationTimeMillis = System.currentTimeMillis() + (expiresIn * 1000L)
                    )
                    saveAccount(updatedAccount)
                    onResult(updatedAccount)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }.start()
    }

    private fun fetchUserProfileAndQuota(accessToken: String, refreshToken: String, expirationMillis: Long): GoogleAccount? {
        try {
            // Fetch User info
            val userRequest = Request.Builder()
                .url(USERINFO_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val userResp = client.newCall(userRequest).execute()
            if (!userResp.isSuccessful) return null
            val userJson = gson.fromJson(userResp.body?.string(), Map::class.java)
            val email = userJson["email"] as? String ?: return null
            val name = userJson["name"] as? String ?: email.substringBefore("@")
            val picture = userJson["picture"] as? String ?: ""

            // Fetch Storage Quota from Drive About endpoint
            var usedBytes = 0L
            var totalBytes = 16106127360L // 15 GB default

            try {
                val driveRequest = Request.Builder()
                    .url(ABOUT_ENDPOINT)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                val driveResp = client.newCall(driveRequest).execute()
                if (driveResp.isSuccessful) {
                    val driveJson = gson.fromJson(driveResp.body?.string(), Map::class.java)
                    val quota = driveJson["storageQuota"] as? Map<*, *>
                    if (quota != null) {
                        usedBytes = (quota["usage"] as? String)?.toLongOrNull() ?: 0L
                        totalBytes = (quota["limit"] as? String)?.toLongOrNull() ?: 16106127360L
                    }
                }
            } catch (e: Exception) {
                // Fallback to defaults if drive about call is limited
            }

            return GoogleAccount(
                email = email,
                displayName = name,
                photoUrl = picture,
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenExpirationTimeMillis = expirationMillis,
                usedStorageBytes = usedBytes,
                totalStorageBytes = totalBytes
            )
        } catch (e: Exception) {
            return null
        }
    }

    // Account Persistence
    fun getSavedAccounts(): List<GoogleAccount> {
        val json = prefs.getString("saved_accounts_json", "[]")
        val type = object : TypeToken<List<GoogleAccount>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAccount(account: GoogleAccount) {
        val current = getSavedAccounts().toMutableList()
        val index = current.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
        if (index >= 0) {
            current[index] = account
        } else {
            current.add(account)
        }
        // Use commit() for refreshes to ensure next read is consistent
        prefs.edit().putString("saved_accounts_json", gson.toJson(current)).commit()
    }

    fun removeAccount(accountEmail: String) {
        val current = getSavedAccounts().filterNot { it.email.equals(accountEmail, ignoreCase = true) }
        prefs.edit().putString("saved_accounts_json", gson.toJson(current)).apply()
    }

    /**
     * Re-fetches only the storage quota for a specific account.
     */
    suspend fun refreshStorageQuota(account: GoogleAccount): GoogleAccount? = withContext(Dispatchers.IO) {
        val validAccount = refreshTokenIfNeededSuspend(account) ?: return@withContext null
        
        try {
            val driveRequest = Request.Builder()
                .url(ABOUT_ENDPOINT)
                .addHeader("Authorization", "Bearer ${validAccount.accessToken}")
                .build()
                
            val driveResp = client.newCall(driveRequest).execute()
            if (driveResp.isSuccessful) {
                val driveJson = gson.fromJson(driveResp.body?.string(), Map::class.java)
                val quota = driveJson["storageQuota"] as? Map<*, *>
                if (quota != null) {
                    val usedBytes = (quota["usage"] as? String)?.toLongOrNull() ?: validAccount.usedStorageBytes
                    val totalBytes = (quota["limit"] as? String)?.toLongOrNull() ?: validAccount.totalStorageBytes
                    
                    val updatedAccount = validAccount.copy(
                        usedStorageBytes = usedBytes,
                        totalStorageBytes = totalBytes
                    )
                    saveAccount(updatedAccount)
                    return@withContext updatedAccount
                }
            }
        } catch (e: Exception) {
            // Log or ignore
        }
        null
    }

    // PKCE Helper Functions
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
