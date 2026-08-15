package com.photomigrate.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.LogItemRow
import com.photomigrate.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    job: TransferJob?,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val currentStatus = job?.status ?: JobStatus.IDLE
    val progress = job?.progress ?: 0f
    val speedKb = (job?.speedBytesPerSec ?: 0L) / 1024L

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text("Transfer Engine", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                actions = {
                    if (currentStatus == JobStatus.COMPLETED) {
                        TextButton(onClick = onDoneClick) {
                            Text("Done", fontWeight = FontWeight.Bold)
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Progress Circular Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 14.dp,
                    color = if (currentStatus == JobStatus.COMPLETED) SuccessGreen else MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "${job?.completedItems ?: 0} of ${job?.totalItems ?: 0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Completion Summary
            if (currentStatus == JobStatus.COMPLETED) {
                val totalSizeMb = (job?.transferredBytes ?: 0L) / (1024L * 1024L)
                val totalSizeGb = totalSizeMb.toDouble() / 1024.0
                val sizeText = if (totalSizeGb >= 1.0) String.format("%.2f GB", totalSizeGb) else "$totalSizeMb MB"
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Migration Complete!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Successfully moved $sizeText of memories.",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDoneClick,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Return to Accounts", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Stats Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(label = "Speed", value = "$speedKb KB/s")
                    StatItem(label = "Mode", value = job?.mode?.name ?: "MOVE")
                    StatItem(label = "Failed", value = "${job?.failedItems ?: 0}")
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Controls
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (currentStatus == JobStatus.RUNNING) {
                        Button(
                            onClick = onPauseClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause")
                        }
                    } else if (currentStatus == JobStatus.PAUSED) {
                        Button(
                            onClick = onResumeClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume")
                        }
                    } else if (currentStatus == JobStatus.COMPLETED) {
                        Button(
                            onClick = onDoneClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finished")
                        }
                    }
                }
            }

            // Logs Section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activity Log",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val logsList = job?.logs?.reversed() ?: emptyList()
                        items(logsList) { log ->
                            LogItemRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}
