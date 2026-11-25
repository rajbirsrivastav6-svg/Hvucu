package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.R
import com.example.data.local.AlbumDao
import com.example.data.local.AlbumEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaItemEntity
import com.example.data.model.Album
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val albumDao: AlbumDao
) {
    val activeMedia: Flow<List<MediaItem>> = mediaDao.getAllActiveMedia().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val favoriteMedia: Flow<List<MediaItem>> = mediaDao.getFavoriteMedia().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val trashMedia: Flow<List<MediaItem>> = mediaDao.getTrashMedia().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val albums: Flow<List<Album>> = combine(
        albumDao.getAllAlbums(),
        mediaDao.getAllActiveMedia()
    ) { albumEntities, mediaEntities ->
        albumEntities.map { albumEntity ->
            val matchingItems = mediaEntities.filter { it.albumName.equals(albumEntity.name, ignoreCase = true) }
            val cover = matchingItems.firstOrNull()?.uriString ?: albumEntity.coverUri
            Album(
                id = albumEntity.id,
                name = albumEntity.name,
                coverUri = cover,
                itemCount = matchingItems.size,
                isSystem = albumEntity.isSystem,
                createdAt = albumEntity.createdAt
            )
        }
    }

    fun getMediaForAlbum(albumName: String): Flow<List<MediaItem>> =
        mediaDao.getMediaByAlbum(albumName).map { list -> list.map { it.toDomainModel() } }

    suspend fun initializeDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val count = mediaDao.getMediaCount()
        if (count == 0) {
            // Seed standard albums
            val defaultAlbums = listOf(
                AlbumEntity(name = "Camera", isSystem = true),
                AlbumEntity(name = "Travel & Nature", isSystem = false),
                AlbumEntity(name = "Urban & Architecture", isSystem = false),
                AlbumEntity(name = "Floral & Macro", isSystem = false),
                AlbumEntity(name = "Wallpapers", isSystem = false),
                AlbumEntity(name = "Edited", isSystem = false)
            )
            albumDao.insertAll(defaultAlbums)

            // Seed initial sample photos using app drawables
            val sampleItems = listOf(
                MediaItemEntity(
                    uriString = "android.resource://${context.packageName}/${R.drawable.img_nature_lake}",
                    title = "Alpine Sunrise Lake",
                    caption = "Serene mountain reflection in mirror-like alpine lake at golden hour.",
                    dateTaken = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
                    sizeBytes = 3_450_000L,
                    width = 4032,
                    height = 3024,
                    isFavorite = true,
                    albumName = "Travel & Nature",
                    tagsString = "Nature, Mountains, Lake, Landscape, Sunrise",
                    location = "Banff National Park, Alberta",
                    cameraModel = "Pixel 9 Pro · 50MP",
                    focalLength = "24mm f/1.68",
                    iso = "ISO 50 · 1/750s"
                ),
                MediaItemEntity(
                    uriString = "android.resource://${context.packageName}/${R.drawable.img_sunset_beach}",
                    title = "Golden Horizon Coast",
                    caption = "Vibrant orange sunset over tropical ocean beach with gentle tide ripples.",
                    dateTaken = System.currentTimeMillis() - 86400000L * 4, // 4 days ago
                    sizeBytes = 2_890_000L,
                    width = 4000,
                    height = 3000,
                    isFavorite = true,
                    albumName = "Travel & Nature",
                    tagsString = "Sunset, Beach, Ocean, Golden Hour, Tropical",
                    location = "Maui, Hawaii",
                    cameraModel = "Pixel 9 Pro · 50MP",
                    focalLength = "48mm f/1.9",
                    iso = "ISO 80 · 1/400s"
                ),
                MediaItemEntity(
                    uriString = "android.resource://${context.packageName}/${R.drawable.img_urban_neon}",
                    title = "Twilight Skyline Geometry",
                    caption = "Modern architectural skyscrapers illuminated against deep twilight blue skies.",
                    dateTaken = System.currentTimeMillis() - 86400000L * 7, // 7 days ago
                    sizeBytes = 3_120_000L,
                    width = 3840,
                    height = 2160,
                    isFavorite = false,
                    albumName = "Urban & Architecture",
                    tagsString = "Architecture, City, Skyscrapers, Urban, Night",
                    location = "Tokyo, Shinjuku District",
                    cameraModel = "Pixel 9 Pro · 50MP",
                    focalLength = "14mm f/2.2 Ultra-wide",
                    iso = "ISO 320 · 1/60s"
                ),
                MediaItemEntity(
                    uriString = "android.resource://${context.packageName}/${R.drawable.img_flower_garden}",
                    title = "Morning Dew Blossom",
                    caption = "Crisp macro focus on blooming petals glistening with morning water droplets.",
                    dateTaken = System.currentTimeMillis() - 86400000L * 10, // 10 days ago
                    sizeBytes = 4_100_000L,
                    width = 4032,
                    height = 3024,
                    isFavorite = true,
                    albumName = "Floral & Macro",
                    tagsString = "Macro, Flowers, Nature, Dew, Botany",
                    location = "Botanical Conservatory",
                    cameraModel = "Pixel 9 Pro · 50MP Macro Mode",
                    focalLength = "24mm f/1.68 · Macro 2cm",
                    iso = "ISO 100 · 1/320s"
                )
            )
            mediaDao.insertAll(sampleItems)
        }
    }

    suspend fun syncDeviceMedia() = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                val itemsToInsert = mutableListOf<MediaItemEntity>()
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val name = c.getString(nameCol) ?: "IMG_$id"
                    val dateTaken = c.getLong(dateCol).let { if (it > 0) it else System.currentTimeMillis() }
                    val size = c.getLong(sizeCol)
                    val width = c.getInt(widthCol).let { if (it > 0) it else 1920 }
                    val height = c.getInt(heightCol).let { if (it > 0) it else 1080 }
                    val mime = c.getString(mimeCol) ?: "image/jpeg"
                    val bucket = c.getString(bucketCol) ?: "Camera"

                    itemsToInsert.add(
                        MediaItemEntity(
                            uriString = contentUri.toString(),
                            title = name.substringBeforeLast("."),
                            caption = "Device Photo",
                            dateTaken = dateTaken,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            mimeType = mime,
                            albumName = bucket,
                            tagsString = "Device, Camera, $bucket",
                            location = "Local Storage",
                            cameraModel = "Device Camera",
                            focalLength = "Standard Lens",
                            iso = "Auto ISO"
                        )
                    )
                }
                if (itemsToInsert.isNotEmpty()) {
                    mediaDao.insertAll(itemsToInsert)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun importMedia(uri: Uri, name: String = "Imported Photo", albumName: String = "Camera") = withContext(Dispatchers.IO) {
        val newItem = MediaItemEntity(
            uriString = uri.toString(),
            title = name,
            caption = "Imported to Gallery",
            dateTaken = System.currentTimeMillis(),
            sizeBytes = 2_150_000L,
            width = 3000,
            height = 2000,
            albumName = albumName,
            tagsString = "Imported, $albumName",
            location = "Device Storage",
            cameraModel = "Photo Import",
            focalLength = "Native",
            iso = "Standard"
        )
        mediaDao.insertMedia(newItem)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun toggleFavoriteBatch(ids: List<Long>, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.updateFavoriteStatusBatch(ids, isFavorite)
    }

    suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        mediaDao.updateTrashStatus(id, true)
    }

    suspend fun moveToTrashBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        mediaDao.updateTrashStatusBatch(ids, true)
    }

    suspend fun restoreFromTrash(id: Long) = withContext(Dispatchers.IO) {
        mediaDao.updateTrashStatus(id, false)
    }

    suspend fun restoreFromTrashBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        mediaDao.updateTrashStatusBatch(ids, false)
    }

    suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) {
        mediaDao.deletePermanently(id)
    }

    suspend fun deletePermanentlyBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        mediaDao.deletePermanentlyBatch(ids)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        mediaDao.emptyTrash()
    }

    suspend fun moveItemsToAlbum(ids: List<Long>, albumName: String) = withContext(Dispatchers.IO) {
        mediaDao.updateAlbumBatch(ids, albumName)
    }

    suspend fun createAlbum(name: String) = withContext(Dispatchers.IO) {
        albumDao.insertAlbum(AlbumEntity(name = name, isSystem = false))
    }

    suspend fun deleteAlbum(id: Long) = withContext(Dispatchers.IO) {
        albumDao.deleteAlbum(id)
    }

    suspend fun updateMedia(item: MediaItem) = withContext(Dispatchers.IO) {
        mediaDao.updateMedia(MediaItemEntity.fromDomainModel(item))
    }

    suspend fun saveEditedMediaAsNew(item: MediaItem, newTitle: String): Long = withContext(Dispatchers.IO) {
        val copy = MediaItemEntity.fromDomainModel(
            item.copy(
                id = 0,
                title = newTitle,
                albumName = "Edited",
                dateTaken = System.currentTimeMillis()
            )
        )
        mediaDao.insertMedia(copy)
    }
}
