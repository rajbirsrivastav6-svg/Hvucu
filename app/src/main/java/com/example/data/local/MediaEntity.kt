package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaItem

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
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
    val tagsString: String = "",
    val location: String = "Captured on Device",
    val cameraModel: String = "Pixel 9 Pro · 50MP",
    val focalLength: String = "24mm f/1.7",
    val iso: String = "ISO 64 · 1/500s",
    val filterType: String = "Normal",
    val rotationAngle: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
) {
    fun toDomainModel(): MediaItem = MediaItem(
        id = id,
        uriString = uriString,
        title = title,
        caption = caption,
        dateTaken = dateTaken,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        mimeType = mimeType,
        isFavorite = isFavorite,
        isTrash = isTrash,
        albumName = albumName,
        tags = if (tagsString.isBlank()) emptyList() else tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        location = location,
        cameraModel = cameraModel,
        focalLength = focalLength,
        iso = iso,
        filterType = filterType,
        rotationAngle = rotationAngle,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation
    )

    companion object {
        fun fromDomainModel(item: MediaItem): MediaItemEntity = MediaItemEntity(
            id = item.id,
            uriString = item.uriString,
            title = item.title,
            caption = item.caption,
            dateTaken = item.dateTaken,
            sizeBytes = item.sizeBytes,
            width = item.width,
            height = item.height,
            mimeType = item.mimeType,
            isFavorite = item.isFavorite,
            isTrash = item.isTrash,
            albumName = item.albumName,
            tagsString = item.tags.joinToString(","),
            location = item.location,
            cameraModel = item.cameraModel,
            focalLength = item.focalLength,
            iso = item.iso,
            filterType = item.filterType,
            rotationAngle = item.rotationAngle,
            brightness = item.brightness,
            contrast = item.contrast,
            saturation = item.saturation
        )
    }
}

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
