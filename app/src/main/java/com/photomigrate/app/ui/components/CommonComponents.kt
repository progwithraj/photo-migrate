package com.photomigrate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photomigrate.app.data.model.AccountRole
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.SyncStatus
import com.photomigrate.app.data.model.TransferLog
import com.photomigrate.app.ui.theme.*

@Composable
fun MeshBackground() {
    val isDark = isSystemInDarkTheme()
    val themeColors = LocalThemeColors.current
    val baseColor = MaterialTheme.colorScheme.background

    val infiniteTransition = rememberInfiniteTransition(label = "MeshPulse")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MeshOffset"
    )

    val accent1 = if (isDark) themeColors.primary.copy(alpha = 0.45f) else themeColors.mesh1
    val accent2 = if (isDark) themeColors.primary.copy(alpha = 0.3f) else themeColors.mesh2
    val accent3 = if (isDark) themeColors.primary.copy(alpha = 0.2f) else themeColors.mesh3

    Box(modifier = Modifier.fillMaxSize().background(baseColor)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(90.dp)) {
            drawCircle(
                color = accent1,
                radius = size.width * 0.95f,
                center = Offset(size.width * 0.2f + pulseOffset, size.height * 0.2f - pulseOffset),
                alpha = 0.45f
            )
            drawCircle(
                color = accent2,
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.8f - pulseOffset, size.height * 0.5f + pulseOffset),
                alpha = 0.35f
            )
            drawCircle(
                color = accent3,
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.3f + pulseOffset, size.height * 0.85f),
                alpha = 0.35f
            )
        }
    }
}

@Composable
fun CredButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val gradient = Brush.horizontalGradient(
        colors = if (enabled) listOf(primaryColor, primaryColor.copy(alpha = 0.82f))
                 else listOf(Color(0xFF2C2D40), Color(0xFF1E1F30))
    )

    val textColor = if (enabled) onPrimaryColor else Color(0xFF636578)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(54.dp)
            .clip(CircleShape),
        color = Color.Transparent,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = textColor
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureBadgeRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeatureBadge(icon = Icons.Default.Shield, label = "100% Safe")
        FeatureBadge(icon = Icons.Default.HighQuality, label = "No Quality Loss")
        FeatureBadge(icon = Icons.Default.Bolt, label = "Fast Transfer")
    }
}

@Composable
fun FeatureBadge(icon: ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                Color.White.copy(alpha = 0.4f)
            )
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.35f else 0.45f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun AccountCard(
    account: GoogleAccount,
    isSelectedAsSource: Boolean,
    isSelectedAsDest: Boolean,
    onSelectRole: (AccountRole) -> Unit,
    onRemoveAccount: () -> Unit
) {
    val usedGb = String.format("%.2f", account.usedStorageBytes.toDouble() / (1024 * 1024 * 1024))
    val totalGb = String.format("%.0f", account.totalStorageBytes.toDouble() / (1024 * 1024 * 1024))
    val percentage = account.usedPercentage

    val storageColor = when {
        percentage >= 0.9f -> ErrorRed
        percentage >= 0.75f -> WarningAmber
        else -> MaterialTheme.colorScheme.primary
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile Picture with glowing ambient ring
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelectedAsSource || isSelectedAsDest) {
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        } else {
                            Brush.linearGradient(colors = listOf(Color.White.copy(0.2f), Color.White.copy(0.05f)))
                        }
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (account.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = account.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.email.take(1).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName.ifEmpty { account.email.substringBefore("@") },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = account.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRemoveAccount,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Usage
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$usedGb GB of $totalGb GB used",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = storageColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp)),
                color = storageColor,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Role Selector - iOS segment style with smooth color animation
        Surface(
            color = Color.Black.copy(alpha = 0.15f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp).height(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleButton(
                    label = "Source",
                    isSelected = isSelectedAsSource,
                    onClick = { onSelectRole(AccountRole.SOURCE) },
                    modifier = Modifier.weight(1f)
                )
                RoleButton(
                    label = "Destination",
                    isSelected = isSelectedAsDest,
                    onClick = { onSelectRole(AccountRole.DESTINATION) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RoleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(250),
        label = "RoleBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "RoleTextColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun MediaItemGridCard(
    item: MediaItem,
    onToggleSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (item.isSelected) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "CardScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (item.isSelected) 2.5.dp else 0.dp,
                color = if (item.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onToggleSelect() }
    ) {
        AsyncImage(
            model = item.thumbnailUrl ?: item.baseUrl,
            contentDescription = item.safeFilename,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // File Size Badge (Frosted Pill)
        val formattedSize = remember(item.sizeBytes) {
            if (item.sizeBytes <= 0) ""
            else {
                val mb = item.sizeBytes.toDouble() / (1024.0 * 1024.0)
                if (mb >= 1024) String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
                else if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
                else String.format(java.util.Locale.US, "%.0f KB", item.sizeBytes.toDouble() / 1024.0)
            }
        }

        if (formattedSize.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formattedSize,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay selection checkmark
        if (item.isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.padding(6.dp).size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }
        }

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("VIDEO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sync Status Badge
        if (item.status != SyncStatus.IDLE) {
            val (icon, color) = when (item.status) {
                SyncStatus.COMPLETED, SyncStatus.TRASHED_FROM_SOURCE -> Icons.Default.Done to SuccessGreen
                SyncStatus.FAILED -> Icons.Default.Error to ErrorRed
                else -> Icons.Default.Sync to MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(color, CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.status.name,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "PremiumLoader")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = 0.3f)
                .border(2.dp, primaryColor, CircleShape)
        )

        // The "Solving Cube" animation
        Canvas(modifier = Modifier.size(48.dp).graphicsLayer(rotationZ = rotation)) {
            val cubeSize = size.width
            drawRect(
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx()),
                size = Size(cubeSize, cubeSize)
            )
            
            // Nested offset squares to give a 3D solving feel
            drawRect(
                color = secondaryColor,
                style = Stroke(width = 2.dp.toPx()),
                size = Size(cubeSize * 0.6f, cubeSize * 0.6f),
                topLeft = Offset(cubeSize * 0.2f, cubeSize * 0.2f)
            )
        }

        // Floating "Data Bits"
        Canvas(modifier = Modifier.size(100.dp).graphicsLayer(rotationZ = -rotation * 0.5f)) {
            val orbitRadius = size.width / 2
            drawCircle(
                color = primaryColor,
                radius = 3.dp.toPx(),
                center = Offset(
                    center.x + orbitRadius * 0.8f,
                    center.y
                )
            )
            drawCircle(
                color = secondaryColor,
                radius = 3.dp.toPx(),
                center = Offset(
                    center.x - orbitRadius * 0.8f,
                    center.y
                )
            )
        }
    }
}

@Composable
fun LogItemRow(log: TransferLog) {
    val color = if (log.isError) ErrorRed else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = log.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
