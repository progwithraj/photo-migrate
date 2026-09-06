package com.photomigrate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.db.PendingCleanup
import com.photomigrate.app.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    items: List<PendingCleanup>,
    onBackClick: () -> Unit,
    onDeleteCloud: (PendingCleanup) -> Unit,
    onDismiss: (PendingCleanup) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Cloud Cleanup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "These files were successfully moved to your destination but failed to be deleted from the source. You can retry trashing them here.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = Color.Green.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("All source accounts are clean!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items) { item ->
                        CleanupItemCard(item, onDeleteCloud, onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
fun CleanupItemCard(
    item: PendingCleanup,
    onDeleteCloud: (PendingCleanup) -> Unit,
    onDismiss: (PendingCleanup) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.filename, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Source Account: ${item.accountId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            if (!item.errorReason.isNullOrEmpty()) {
                Text("Reason: ${item.errorReason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDismiss(item) }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onDeleteCloud(item) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Trash")
                }
            }
        }
    }
}
