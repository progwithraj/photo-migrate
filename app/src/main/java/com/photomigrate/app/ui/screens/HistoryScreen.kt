package com.photomigrate.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.data.model.TransferLog
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<TransferJob>,
    totalBytes: Long,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    onFetchLogs: suspend (String) -> List<TransferLog>
) {
    var selectedJobId by remember { mutableStateOf<String?>(null) }
    var selectedJobLogs by remember { mutableStateOf<List<TransferLog>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Transfer History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global Analytics Header
            AnalyticsHeader(totalBytes, history.size)

            if (history.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(history) { job ->
                        HistoryJobCard(
                            job = job,
                            onClick = {
                                scope.launch {
                                    selectedJobLogs = onFetchLogs(job.id)
                                    selectedJobId = job.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Logs Detail Overlay
    if (selectedJobId != null) {
        AlertDialog(
            onDismissRequest = { selectedJobId = null },
            title = { Text("Job Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.height(400.dp)) {
                    val job = history.find { it.id == selectedJobId }
                    if (job != null) {
                        Text("Status: ${job.status}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Moved: ${job.completedItems} / ${job.totalItems}", fontSize = 14.sp)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        items(selectedJobLogs) { log ->
                            Text(
                                text = "> ${log.message}",
                                fontSize = 11.sp,
                                color = if (log.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedJobId = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AnalyticsHeader(totalBytes: Long, jobCount: Int) {
    val totalMb = totalBytes.toDouble() / (1024.0 * 1024.0)
    val totalGb = totalMb / 1024.0
    
    // Switch to GB only if we have at least 100MB
    val showInGb = totalGb >= 0.1
    val displayValue = if (showInGb) totalGb else totalMb
    val unitText = if (showInGb) " GB" else " MB"

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Lifetime Stats", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.2f", displayValue),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = unitText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text("Total Data Migrated", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("$jobCount Jobs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun HistoryJobCard(job: TransferJob, onClick: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(job.startTime))
    val statusColor = when (job.status) {
        JobStatus.COMPLETED -> SuccessGreen
        JobStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (job.mode == com.photomigrate.app.data.model.TransferMode.MOVE) Icons.Default.MoveUp else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = job.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${job.completedItems} / ${job.totalItems} Items", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val sizeMb = job.transferredBytes / (1024L * 1024L)
                    Text("$sizeMb MB Migrated", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No History Yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Start a migration to see it here.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
