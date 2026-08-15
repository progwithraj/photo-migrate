package com.photomigrate.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.data.repository.TransferRepository
import com.photomigrate.app.service.TransferWorker
import com.photomigrate.app.ui.components.MeshBackground
import com.photomigrate.app.ui.screens.*
import com.photomigrate.app.ui.theme.PhotoMigrateTheme
import com.photomigrate.app.ui.theme.ThemeManager
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private lateinit var oauthManager: OAuthManager
    private lateinit var repository: TransferRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oauthManager = OAuthManager(this)
        repository = TransferRepository.getInstance(this)

        handleOAuthRedirect(intent)

        setContent {
            PhotoMigrateTheme(appTheme = ThemeManager.currentTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MeshBackground()
                    
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        val navController = rememberNavController()

                        var accounts by remember { mutableStateOf(oauthManager.getSavedAccounts()) }
                        var selectedSourceId by remember { mutableStateOf<String?>(accounts.firstOrNull()?.id) }
                        var selectedDestId by remember { mutableStateOf<String?>(accounts.getOrNull(1)?.id) }

                        val sourceMediaList by repository.sourceMediaList.collectAsState()
                        val isLoadingMedia by repository.isLoadingMedia.collectAsState()
                        val currentJob by repository.currentJob.collectAsState()

                        NavHost(navController = navController, startDestination = "accounts") {
                            composable("accounts") {
                                AccountScreen(
                                    accounts = accounts,
                                    selectedSourceId = selectedSourceId,
                                    selectedDestId = selectedDestId,
                                    onSelectSourceAccount = { selectedSourceId = it },
                                    onSelectDestAccount = { selectedDestId = it },
                                    onAddAccountClick = {
                                        val authUrl = oauthManager.generateAuthUrl()
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                        startActivity(browserIntent)
                                    },
                                    onOpenSetupGuide = { navController.navigate("settings") },
                                    onRemoveAccount = { email ->
                                        oauthManager.removeAccount(email)
                                        accounts = oauthManager.getSavedAccounts()
                                        if (selectedSourceId == accounts.find { it.email == email }?.id) selectedSourceId = null
                                        if (selectedDestId == accounts.find { it.email == email }?.id) selectedDestId = null
                                    },
                                    onRefreshAll = {
                                        lifecycleScope.launch {
                                            val currentAccs = oauthManager.getSavedAccounts()
                                            coroutineScope {
                                                currentAccs.map { account ->
                                                    async { oauthManager.refreshStorageQuota(account) }
                                                }.awaitAll()
                                            }
                                            accounts = oauthManager.getSavedAccounts()
                                            Toast.makeText(this@MainActivity, "Storage usage updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onProceedToPicker = {
                                        val sourceAccount = accounts.find { it.id == selectedSourceId }
                                        val destAccount = accounts.find { it.id == selectedDestId }
                                        if (sourceAccount != null) {
                                            // Load media in background
                                            lifecycleScope.launch {
                                                repository.loadSourceMedia(sourceAccount, destAccount)
                                            }
                                            navController.navigate("picker")
                                        }
                                    }
                                )
                            }

                            composable("picker") {
                                MediaPickerScreen(
                                    mediaItems = sourceMediaList,
                                    isLoading = isLoadingMedia,
                                    onBackClick = { navController.popBackStack() },
                                    onStartTransfer = { mode, selectedItems ->
                                        val sourceAcc = accounts.find { it.id == selectedSourceId }
                                        val destAcc = accounts.find { it.id == selectedDestId }

                                        if (sourceAcc != null && destAcc != null) {
                                            lifecycleScope.launch {
                                                val job = repository.createAndStartJob(sourceAcc, destAcc, mode, selectedItems)

                                                // Enqueue WorkManager background worker
                                                // Pass only Job ID to bypass WorkManager data limits
                                                val workData = Data.Builder()
                                                    .putString(TransferWorker.KEY_JOB_ID, job.id)
                                                    .putString(TransferWorker.KEY_SOURCE_ACCOUNT_ID, sourceAcc.id)
                                                    .putString(TransferWorker.KEY_DEST_ACCOUNT_ID, destAcc.id)
                                                    .putString(TransferWorker.KEY_MODE, mode.name)
                                                    .build()

                                                val workRequest = OneTimeWorkRequestBuilder<TransferWorker>()
                                                    .setInputData(workData)
                                                    .build()

                                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                                                    "photo_transfer_work",
                                                    androidx.work.ExistingWorkPolicy.REPLACE,
                                                    workRequest
                                                )

                                                navController.navigate("transfer")
                                            }
                                        }
                                    }
                                )
                            }

                            composable("transfer") {
                                TransferScreen(
                                    job = currentJob,
                                    onPauseClick = { repository.pauseJob() },
                                    onResumeClick = { repository.resumeJob() },
                                    onDoneClick = {
                                        // Refresh storage usage for the accounts involved in transfer
                                        lifecycleScope.launch {
                                            val currentAccs = oauthManager.getSavedAccounts()
                                            selectedSourceId?.let { id ->
                                                currentAccs.find { it.id == id }?.let { oauthManager.refreshStorageQuota(it) }
                                            }
                                            selectedDestId?.let { id ->
                                                currentAccs.find { it.id == id }?.let { oauthManager.refreshStorageQuota(it) }
                                            }
                                            // Update local state to trigger UI refresh on Home Screen
                                            accounts = oauthManager.getSavedAccounts()
                                        }
                                        navController.popBackStack("accounts", inclusive = false)
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    currentClientId = oauthManager.getClientId(),
                                    currentClientSecret = oauthManager.getClientSecret(),
                                    onSaveCredentials = { clientId, clientSecret ->
                                        oauthManager.saveClientCredentials(clientId, clientSecret)
                                        Toast.makeText(this@MainActivity, "Credentials Saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        Log.d("PhotoMigrate", "Received URI: $uri")
        if (uri.scheme == OAuthManager.REDIRECT_URI_SCHEME) {
            val code = uri.getQueryParameter("code")
            val verifier = oauthManager.getPendingVerifier()
            Log.d("PhotoMigrate", "Code found, exchanging... verifier: $verifier")

            if (!code.isNullOrEmpty()) {
                // Clear intent data to prevent double-exchange on activity recreation
                intent.data = null
                
                oauthManager.exchangeCodeForToken(code, verifier) { account, error ->
                    runOnUiThread {
                        if (account != null) {
                            Log.d("PhotoMigrate", "Success! Account: ${account.email}")
                            Toast.makeText(this, "Connected ${account.email}!", Toast.LENGTH_LONG).show()
                            recreate() 
                        } else {
                            Log.e("PhotoMigrate", "Auth Error: $error")
                            Toast.makeText(this, error ?: "Authentication failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            Log.w("PhotoMigrate", "URI mismatch. Scheme: ${uri.scheme}, Host: ${uri.host}")
        }
    }
}
