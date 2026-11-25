package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem

enum class EditorTab {
    FILTERS,
    ADJUST,
    CROP_ROTATE
}

@Composable
fun PhotoEditorScreen(
    item: MediaItem,
    onClose: () -> Unit,
    onSave: (edited: MediaItem, saveAsCopy: Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(EditorTab.FILTERS) }
    var currentFilter by remember { mutableStateOf(item.filterType) }
    var rotationAngle by remember { mutableFloatStateOf(item.rotationAngle) }
    var brightness by remember { mutableFloatStateOf(item.brightness) }
    var contrast by remember { mutableFloatStateOf(item.contrast) }
    var saturation by remember { mutableFloatStateOf(item.saturation) }
    var cropAspectRatio by remember { mutableStateOf<Float?>(null) } // null = original

    // Compute Color Filter Matrix based on preset + adjustments
    val colorFilter = remember(currentFilter, brightness, contrast, saturation) {
        buildColorFilter(currentFilter, brightness, contrast, saturation)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Editor Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Edit Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            currentFilter = "Normal"
                            rotationAngle = 0f
                            brightness = 0f
                            contrast = 1f
                            saturation = 1f
                            cropAspectRatio = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = Color.LightGray
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val updated = item.copy(
                                filterType = currentFilter,
                                rotationAngle = rotationAngle,
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation
                            )
                            onSave(updated, false)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }

            // Main Photo Canvas Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val imageModifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (cropAspectRatio != null) {
                            Modifier.aspectRatio(cropAspectRatio!!)
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uriString)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                    modifier = imageModifier
                )
            }

            // Bottom Controls Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF16181E),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    // Category Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color(0xFF16181E),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == EditorTab.FILTERS,
                            onClick = { selectedTab = EditorTab.FILTERS },
                            text = { Text("Filters") },
                            icon = { Icon(Icons.Default.Filter, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )
                        Tab(
                            selected = selectedTab == EditorTab.ADJUST,
                            onClick = { selectedTab = EditorTab.ADJUST },
                            text = { Text("Adjust") },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )
                        Tab(
                            selected = selectedTab == EditorTab.CROP_ROTATE,
                            onClick = { selectedTab = EditorTab.CROP_ROTATE },
                            text = { Text("Transform") },
                            icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Contents
                    when (selectedTab) {
                        EditorTab.FILTERS -> {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(FilterPreset.values()) { preset ->
                                    val isSelected = currentFilter == preset.displayName
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { currentFilter = preset.displayName }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(item.uriString)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = preset.displayName,
                                                contentScale = ContentScale.Crop,
                                                colorFilter = buildPresetMatrix(preset.displayName),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        EditorTab.ADJUST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                // Brightness
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Brightness", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${(brightness * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -0.5f..0.5f
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Contrast
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Contrast", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${(contrast * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0.5f..2.0f
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Saturation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saturation", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${(saturation * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Slider(
                                    value = saturation,
                                    onValueChange = { saturation = it },
                                    valueRange = 0.0f..2.0f
                                )
                            }
                        }

                        EditorTab.CROP_ROTATE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                // Rotation Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { rotationAngle = (rotationAngle - 90f) % 360f },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rotate -90°", color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    OutlinedButton(
                                        onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rotate +90°", color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Aspect Ratio Preset Chips
                                Text(
                                    "Aspect Ratio Presets",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val ratios = listOf(
                                        "Free" to null,
                                        "1:1" to 1.0f,
                                        "4:3" to 4f / 3f,
                                        "16:9" to 16f / 9f,
                                        "9:16" to 9f / 16f
                                    )
                                    ratios.forEach { (label, ratio) ->
                                        FilterChip(
                                            selected = cropAspectRatio == ratio,
                                            onClick = { cropAspectRatio = ratio },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Save Option: "Save as Copy"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                val updated = item.copy(
                                    filterType = currentFilter,
                                    rotationAngle = rotationAngle,
                                    brightness = brightness,
                                    contrast = contrast,
                                    saturation = saturation
                                )
                                onSave(updated, true)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as New Copy", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

private fun buildPresetMatrix(filterType: String): ColorFilter? {
    val matrix = ColorMatrix()
    when (filterType) {
        "Warm Glow" -> {
            matrix.setToScale(1.15f, 1.05f, 0.9f, 1f)
        }
        "Cool Breeze" -> {
            matrix.setToScale(0.9f, 1.05f, 1.2f, 1f)
        }
        "B&W Mono" -> {
            matrix.setToSaturation(0f)
        }
        "Vivid Pop" -> {
            matrix.setToSaturation(1.6f)
        }
        "Retro Film" -> {
            matrix.setToScale(1.1f, 0.95f, 0.85f, 1f)
            matrix.setToSaturation(0.85f)
        }
        "Dramatic" -> {
            matrix.setToSaturation(1.2f)
            matrix.setToScale(1.1f, 1.1f, 1.1f, 1f)
        }
        "Sepia Nostalgia" -> {
            val sepia = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            return ColorFilter.colorMatrix(ColorMatrix(sepia))
        }
        "Neon Noir" -> {
            matrix.setToScale(1.3f, 0.8f, 1.4f, 1f)
            matrix.setToSaturation(1.5f)
        }
        else -> return null
    }
    return ColorFilter.colorMatrix(matrix)
}

private fun buildColorFilter(
    filterType: String,
    brightness: Float,
    contrast: Float,
    saturation: Float
): ColorFilter? {
    val matrix = ColorMatrix()

    // 1. Apply Preset Base
    when (filterType) {
        "Warm Glow" -> matrix.setToScale(1.15f, 1.05f, 0.9f, 1f)
        "Cool Breeze" -> matrix.setToScale(0.9f, 1.05f, 1.2f, 1f)
        "B&W Mono" -> matrix.setToSaturation(0f)
        "Vivid Pop" -> matrix.setToSaturation(1.6f)
        "Retro Film" -> {
            matrix.setToScale(1.1f, 0.95f, 0.85f, 1f)
        }
        "Sepia Nostalgia" -> {
            val sepia = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            return ColorFilter.colorMatrix(ColorMatrix(sepia))
        }
        "Neon Noir" -> {
            matrix.setToScale(1.3f, 0.8f, 1.4f, 1f)
        }
    }

    // 2. Apply Custom Saturation
    if (saturation != 1f && filterType != "B&W Mono") {
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(saturation)
        matrix.timesAssign(satMatrix)
    }

    // 3. Apply Brightness offset (values in matrix are scaled up by 255 for offsets)
    if (brightness != 0f || contrast != 1f) {
        val scale = contrast
        val translate = (brightness * 255f) + ((1f - contrast) * 128f)
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
