package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ExifInfoSheet
import com.example.ui.components.SlideshowControlsBar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    initialSlideshowSpeedSeconds: Int = 3,
    confirmBeforeDelete: Boolean = true,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToTrash: (MediaItem) -> Unit,
    onOpenEditor: (MediaItem) -> Unit,
    onSaveDetails: (item: MediaItem, caption: String, tags: List<String>) -> Unit
) {
    if (mediaList.isEmpty()) {
        onClose()
        return
    }

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, mediaList.size - 1)) }
    val currentItem = mediaList.getOrNull(currentIndex) ?: return

    var showControls by remember { mutableStateOf(true) }
    var isSlideshowPlaying by remember { mutableStateOf(false) }
    var slideshowSpeedSeconds by remember(initialSlideshowSpeedSeconds) {
        mutableIntStateOf(initialSlideshowSpeedSeconds)
    }
    var showExifSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Gesture zoom and pan state
    var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
    var offset by remember(currentIndex) { mutableStateOf(Offset.Zero) }

    val exifSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Slideshow loop
    LaunchedEffect(isSlideshowPlaying, currentIndex, slideshowSpeedSeconds) {
        if (isSlideshowPlaying) {
            delay(slideshowSpeedSeconds * 1000L)
            if (currentIndex < mediaList.size - 1) {
                currentIndex++
            } else {
                currentIndex = 0 // loop back to first
            }
        }
    }

    val formattedDate = remember(currentItem.dateTaken) {
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        sdf.format(Date(currentItem.dateTaken))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Image Area with Zoom, Pan, Double-Tap & Swipe Gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentIndex) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.2f
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentItem.uriString)
                    .crossfade(true)
                    .build(),
                contentDescription = currentItem.title,
                contentScale = ContentScale.Fit,
                colorFilter = buildItemColorFilter(currentItem),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        rotationZ = currentItem.rotationAngle
                    }
            )
        }

        // Left / Right Navigation Touch Controls (visible when zoomed out)
        if (scale == 1f && showControls) {
            if (currentIndex > 0) {
                IconButton(
                    onClick = { currentIndex-- },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Photo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (currentIndex < mediaList.size - 1) {
                IconButton(
                    onClick = { currentIndex++ },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Photo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Top App Bar Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentItem.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentIndex + 1} of ${mediaList.size} · $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    Row {
                        IconButton(onClick = { onToggleFavorite(currentItem) }) {
                            Icon(
                                imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentItem.isFavorite) Color(0xFFFF5252) else Color.White
                            )
                        }
                        IconButton(onClick = { showExifSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Photo Info",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar Controls
        AnimatedVisibility(
            visible = showControls && !isSlideshowPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slideshow Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(4.dp)
                    ) {
                        IconButton(onClick = { isSlideshowPlaying = true }) {
                            Icon(
                                imageVector = Icons.Default.Slideshow,
                                contentDescription = "Slideshow",
                                tint = Color.White
                            )
                        }
                        Text("Slideshow", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }

                    // Edit Photo Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(4.dp)
                    ) {
                        IconButton(onClick = { onOpenEditor(currentItem) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("Edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    // Share Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = currentItem.mimeType
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(currentItem.uriString))
                                        putExtra(Intent.EXTRA_TEXT, "Photo: ${currentItem.title}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                        Text("Share", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }

                    // Delete Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (confirmBeforeDelete) {
                                    showDeleteConfirmDialog = true
                                } else {
                                    onMoveToTrash(currentItem)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5252)
                            )
                        }
                        Text("Delete", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5252))
                    }
                }
            }
        }

        // Active Slideshow Floating Bar
        if (isSlideshowPlaying) {
            SlideshowControlsBar(
                isPlaying = isSlideshowPlaying,
                onTogglePlay = { isSlideshowPlaying = !isSlideshowPlaying },
                onClose = { isSlideshowPlaying = false },
                speedSeconds = slideshowSpeedSeconds,
                onChangeSpeed = { slideshowSpeedSeconds = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }

        // EXIF Sheet
        if (showExifSheet) {
            ExifInfoSheet(
                item = currentItem,
                sheetState = exifSheetState,
                onDismiss = { showExifSheet = false },
                onSaveDetails = { caption, tags ->
                    onSaveDetails(currentItem, caption, tags)
                }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirmDialog) {
            ConfirmActionDialog(
                title = "Move to Trash?",
                message = "This photo will be moved to the Trash bin. You can restore it anytime.",
                confirmText = "Move to Trash",
                isDestructive = true,
                onDismiss = { showDeleteConfirmDialog = false },
                onConfirm = {
                    onMoveToTrash(currentItem)
                }
            )
        }
    }
}

private fun buildItemColorFilter(item: MediaItem): ColorFilter? {
    if (item.filterType == "Normal" && item.brightness == 0f && item.contrast == 1f && item.saturation == 1f) {
        return null
    }
    val matrix = ColorMatrix()
    when (item.filterType) {
        "Warm Glow" -> matrix.setToScale(1.15f, 1.05f, 0.9f, 1f)
        "Cool Breeze" -> matrix.setToScale(0.9f, 1.05f, 1.2f, 1f)
        "B&W Mono" -> matrix.setToSaturation(0f)
        "Vivid Pop" -> matrix.setToSaturation(1.6f)
        "Retro Film" -> matrix.setToScale(1.1f, 0.95f, 0.85f, 1f)
        "Sepia Nostalgia" -> {
            val sepia = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            return ColorFilter.colorMatrix(ColorMatrix(sepia))
        }
        "Neon Noir" -> matrix.setToScale(1.3f, 0.8f, 1.4f, 1f)
    }

    if (item.saturation != 1f && item.filterType != "B&W Mono") {
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(item.saturation)
        matrix.timesAssign(satMatrix)
    }

    if (item.brightness != 0f || item.contrast != 1f) {
        val scale = item.contrast
        val translate = (item.brightness * 255f) + ((1f - item.contrast) * 128f)
        val adjMatrix = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        matrix.timesAssign(ColorMatrix(adjMatrix))
    }

    return ColorFilter.colorMatrix(matrix)
}
