package com.photomigrate.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    
    val remainingSec = (job?.remainingTimeMillis ?: 0L) / 1000L
    val remainingText = if (remainingSec > 60) "${remainingSec / 60} min" else "${remainingSec} sec"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text("Transfer Engine", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                actions = {
                    if (currentStatus == JobStatus.COMPLETED || currentStatus == JobStatus.FAILED) {
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
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "TransferProgress"
                )

                val progressColor = when (currentStatus) {
                    JobStatus.COMPLETED -> SuccessGreen
                    JobStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 14.dp,
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
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
            if (currentStatus == JobStatus.COMPLETED || currentStatus == JobStatus.FAILED) {
                val totalSizeMb = (job?.transferredBytes ?: 0L) / (1024L * 1024L)
                val totalSizeGb = totalSizeMb.toDouble() / 1024.0
                val sizeText = if (totalSizeGb >= 1.0) String.format(java.util.Locale.US, "%.2f GB", totalSizeGb) else "$totalSizeMb MB"
                
                val isSuccess = currentStatus == JobStatus.COMPLETED && (job?.completedItems ?: 0) > 0
                val statusTitle = if (isSuccess) "Migration Complete!" else "Migration Failed"
                val statusColor = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.error
                val statusIcon = if (isSuccess) Icons.Default.AutoFixHigh else Icons.Default.ErrorOutline
                val statusDesc = if (isSuccess) "Successfully moved $sizeText of memories." else "Could not transfer media. Check the logs below."

                GlassCard(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusTitle,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusDesc,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDoneClick,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                        ) {
                            Text(if (isSuccess) "Return to Accounts" else "Retry / Go Back", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Stats Card with Live Graph
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(label = "Speed", value = "$speedKb KB/s")
                    StatItem(label = "Remaining", value = remainingText)
                    StatItem(label = "Failed", value = "${job?.failedItems ?: 0}")
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // ECG Trend Line in its own row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    NetworkSpeedGraph(
                        speedHistory = job?.speedHistory ?: emptyList(),
                        color = MaterialTheme.colorScheme.primary
                    )
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onResumeClick,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resume")
                            }
                            Button(
                                onClick = onDoneClick,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Error, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop")
                            }
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

            // Activity Log
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
fun NetworkSpeedGraph(
    speedHistory: List<Long>,
    color: Color
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        if (speedHistory.isEmpty()) return@Canvas
        val maxSpeed = (speedHistory.maxOrNull() ?: 1L).coerceAtLeast(1024L)
        val width = size.width
        val height = size.height
        // Dynamic stepX so the graph always covers the full width of the container
        val stepX = width / (speedHistory.size - 1).coerceAtLeast(1)

        val path = androidx.compose.ui.graphics.Path().apply {
            speedHistory.forEachIndexed { index, speed ->
                val x = index * stepX
                val y = height - (speed.toFloat() / maxSpeed.toFloat() * height).coerceIn(0f, height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // Draw the filled area (nPerf style)
        if (speedHistory.size >= 2) {
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height)
                speedHistory.forEachIndexed { index, speed ->
                    val x = index * stepX
                    val y = height - (speed.toFloat() / maxSpeed.toFloat() * height).coerceIn(0f, height)
                    lineTo(x, y)
                }
                lineTo((speedHistory.size - 1) * stepX, height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.4f)),
                    startY = 0f,
                    endY = height
                )
            )
        }

        // Draw the top line
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}
