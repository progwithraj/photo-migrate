package com.photomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentClientId: String,
    currentClientSecret: String,
    onSaveCredentials: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val oauthManager = remember { OAuthManager(context) }
    
    var clientId by remember { mutableStateOf(currentClientId) }
    var clientSecret by remember { mutableStateOf(currentClientSecret) }
    var savedSuccess by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Appearance
            Text(
                text = "Appearance",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Customize Theme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeManager.themes.forEach { themeOption ->
                        val colors = ThemeManager.getThemeColors(themeOption)
                        val isSelected = ThemeManager.currentTheme == themeOption

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(colors.primary, colors.mesh1)
                                    )
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSystemInDarkTheme()) Color.White else Color.Black.copy(
                                        alpha = 0.5f
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { ThemeManager.setTheme(themeOption) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section: Optimization
            Text(
                text = "Transfer Optimization",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Storage Saver Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val currentOptPref = remember { mutableStateOf(oauthManager.getOptimizationPreference()) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        OAuthManager.OPT_ASK to "Ask Everytime",
                        OAuthManager.OPT_YES to "Yes, Optimize",
                        OAuthManager.OPT_NO to "Original Only"
                    )

                    options.forEach { (pref, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentOptPref.value = pref
                                    oauthManager.setOptimizationPreference(pref)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentOptPref.value == pref,
                                onClick = {
                                    currentOptPref.value = pref
                                    oauthManager.setOptimizationPreference(pref)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label, 
                                fontSize = 15.sp, 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This setting controls whether you are prompted to compress images before each transfer.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Section: AI Features
            Text(
                text = "AI Features (Experimental)",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                var aiEnabled by remember { mutableStateOf(oauthManager.isAiOrgEnabled()) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Organization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Use on-device AI to automatically group photos into albums based on their content (e.g., Nature, Pets).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = aiEnabled,
                        onCheckedChange = {
                            aiEnabled = it
                            oauthManager.setAiOrgEnabled(it)
                        }
                    )
                }
            }

            // Section: Advanced
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Advanced Settings",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Custom OAuth Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Configure your own Google Cloud Console credentials for higher rate limits.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // OAuth Form
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("OAuth Client ID") },
                        placeholder = { Text("e.g. 12345.apps.googleusercontent.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientSecret,
                        onValueChange = { clientSecret = it },
                        label = { Text("OAuth Client Secret") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onSaveCredentials(clientId, clientSecret)
                            savedSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save API Credentials")
                    }

                    if (savedSuccess) {
                        Text(
                            "Credentials saved! Please restart auth flow if needed.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connection Guide:", fontWeight = FontWeight.Bold)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "1. Enable Photos & Drive APIs in Google Cloud Console.",
                                fontSize = 12.sp
                            )
                            Text("2. Create Android OAuth Client ID.", fontSize = 12.sp)
                            Text(
                                "3. Add Redirect URI: com.photomigrate.app://oauthredirect",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "4. Important: When signing in, ensure you check the box for 'See, edit, create, and delete all your Google Drive files' to enable MOVE mode.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
