package com.photomigrate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    currentClientId: String,
    currentClientSecret: String,
    onSaveCredentials: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var clientId by remember { mutableStateOf(currentClientId) }
    var clientSecret by remember { mutableStateOf(currentClientSecret) }
    var savedSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Free Setup Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100% Free Forever", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "This app connects directly from your device to Google APIs without paid servers. You simply need a free Google Cloud API key.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text("3 Easy Steps to Connect:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Step 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 1: Open Google Cloud Console", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Go to console.cloud.google.com on your phone or PC and sign in with any free Google account. Create a new free project named 'PhotoMigrate'.",
                        fontSize = 13.sp
                    )
                }
            }

            // Step 2
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 2: Enable Google Photos & Drive APIs", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "In your project, navigate to 'APIs & Services' -> 'Library'. Search for 'Google Photos Library API' and 'Google Drive API' and click Enable (Free quota: 10,000 requests/day).",
                        fontSize = 13.sp
                    )
                }
            }

            // Step 3
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 3: Create OAuth Client ID", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Go to 'Credentials' -> 'Create Credentials' -> 'OAuth client ID'. Select Application type: Android or Web Application. Add Redirect URI:\ncom.photomigrate.app://oauthredirect",
                        fontSize = 13.sp
                    )
                }
            }

            // Enter Credentials Form
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Enter Your Google API Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("OAuth Client ID") },
                    placeholder = { Text("e.g. 123456789-abc.apps.googleusercontent.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    label = { Text("OAuth Client Secret (Optional for Android)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSaveCredentials(clientId, clientSecret)
                        savedSuccess = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Credentials")
                }

                if (savedSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Credentials saved successfully! Return to Accounts screen to sign in.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
