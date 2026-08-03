package com.photomigrate.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.LogItemRow
import com.photomigrate.app.ui.theme.PrimaryBlue
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
        topBar = {
            TopAppBar(
                title = { Text("Photo Transfer Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    if (currentStatus == JobStatus.COMPLETED) {
                        TextButton(onClick = onDoneClick) {
                            Text("Done")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Progress Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = if (currentStatus == JobStatus.COMPLETED) SuccessGreen else PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text(
                                text = "${job?.completedItems ?: 0} / ${job?.totalItems ?: 0}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transfer Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Transfer Speed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${speedKb} KB/s", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(job?.mode?.name ?: "MOVE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed/Skipped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${job?.failedItems ?: 0}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStatus == JobStatus.RUNNING) {
                            Button(onClick = onPauseClick) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause")
                            }
                        } else if (currentStatus == JobStatus.PAUSED) {
                            Button(onClick = onResumeClick) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume")
                            }
                        } else if (currentStatus == JobStatus.COMPLETED) {
                            Button(onClick = onDoneClick, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transfer Complete")
                            }
                        }
                    }
                }
            }

            // Real-Time Log Console
            Text(
                text = "Live Activity Log",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
