package com.photomigrate.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.data.model.TransferLog
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.theme.CredNeonPink
import com.photomigrate.app.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<TransferJob>,
    totalBytes: Long,
    hasPendingCleanups: Boolean,
    onBackClick: () -> Unit,
    onOpenCleanup: () -> Unit,
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
                    if (hasPendingCleanups) {
                        IconButton(onClick = onOpenCleanup) {
                            BadgedBox(
                                badge = { Badge { Text("!") } }
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Pending Cleanups")
                            }
                        }
                    }
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(padding.calculateTopPadding() - 8.dp))
            
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

    // CRED-Style Transfer Details Modal Sheet
    if (selectedJobId != null) {
        val selectedJob = history.find { it.id == selectedJobId }
        
        ModalBottomSheet(
            onDismissRequest = { selectedJobId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0D0E19),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2E42))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transfer Details",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    IconButton(onClick = { selectedJobId = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                if (selectedJob != null) {
                    val statusColor = when (selectedJob.status) {
                        JobStatus.COMPLETED -> SuccessGreen
                        JobStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> CredNeonPink
                    }

                    // Status Chip & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedJob.status.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        val date = SimpleDateFormat("Sept dd, HH:mm", Locale.US).format(Date(selectedJob.startTime))
                        Text(date, fontSize = 13.sp, color = Color(0xFF9394A5))
                    }

                    // Item & Size Summary
                    val sizeMb = selectedJob.transferredBytes / (1024L * 1024L)
                    Text(
                        text = "${selectedJob.completedItems} / ${selectedJob.totalItems} items (${sizeMb} MB Migrated)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    HorizontalDivider(color = Color(0xFF23253B))

                    // Account Nodes Connector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131422), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF23253B), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = CredNeonPink.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("From", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CredNeonPink)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedJob.sourceAccountId.substringBefore("@"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = CredNeonPink,
                            modifier = Modifier.size(24.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("To", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedJob.destinationAccountId.substringBefore("@"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Transfer Summary Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailMetric("Mode", selectedJob.mode.name)
                        DetailMetric("Quality", if (selectedJob.isCompressionEnabled) "Storage Saver" else "Original")
                        DetailMetric("AI Org", selectedJob.orgMode.name)
                    }

                    // Monospaced Activity Logs
                    Text("Technical Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFF080910), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF23253B), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        items(selectedJobLogs) { log ->
                            Text(
                                text = "> ${log.message}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.isError) MaterialTheme.colorScheme.error else Color(0xFF9394A5),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color(0xFF9394A5))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun AnalyticsHeader(totalBytes: Long, jobCount: Int) {
    val totalMb = totalBytes.toDouble() / (1024.0 * 1024.0)
    val totalGb = totalMb / 1024.0
    
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
                Text("Lifetime Stats", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CredNeonPink)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", displayValue),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = unitText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9394A5),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text("Total Data Migrated", fontSize = 12.sp, color = Color(0xFF9394A5))
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = CredNeonPink.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = CredNeonPink)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("$jobCount Jobs", fontWeight = FontWeight.Bold, color = Color.White)
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
        else -> CredNeonPink
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131422)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF23253B))
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
                        tint = Color(0xFF9394A5)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(date, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
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
                    Text("${job.completedItems} / ${job.totalItems} Items", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    val sizeMb = job.transferredBytes / (1024L * 1024L)
                    Text("$sizeMb MB Migrated", fontSize = 12.sp, color = Color(0xFF9394A5))
                }
                
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9394A5))
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
            tint = Color(0xFF23253B)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No History Yet", fontWeight = FontWeight.Bold, color = Color.White)
        Text("Start a migration to see it here.", fontSize = 14.sp, color = Color(0xFF9394A5))
    }
}
