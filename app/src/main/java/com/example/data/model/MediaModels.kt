package com.example.data.model

data class MediaItem(
    val id: Long = 0,
    val uriString: String,
    val title: String,
    val caption: String = "",
    val dateTaken: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 2_450_000L,
    val width: Int = 1920,
    val height: Int = 1440,
    val mimeType: String = "image/jpeg",
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val albumName: String = "Camera",
    val tags: List<String> = emptyList(),
    val location: String = "Captured on Device",
    val cameraModel: String = "Pixel 9 Pro · 50MP",
    val focalLength: String = "24mm f/1.7",
    val iso: String = "ISO 64 · 1/500s",
    val filterType: String = "Normal",
    val rotationAngle: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
)

data class Album(
    val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val itemCount: Int = 0,
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FilterPreset(val displayName: String) {
    NORMAL("Original"),
    WARM("Warm Glow"),
    COOL("Cool Breeze"),
    MONO("B&W Mono"),
    VIVID("Vivid Pop"),
    VINTAGE("Retro Film"),
    DRAMATIC("Dramatic"),
    SEPIA("Sepia Nostalgia"),
    CYBERPUNK("Neon Noir")
}
