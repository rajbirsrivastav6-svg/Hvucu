package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE isTrash = 0 ORDER BY dateTaken DESC")
    fun getAllActiveMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isFavorite = 1 ORDER BY dateTaken DESC")
    fun getFavoriteMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 1 ORDER BY dateTaken DESC")
    fun getTrashMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND albumName = :albumName ORDER BY dateTaken DESC")
    fun getMediaByAlbum(albumName: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaItemEntity?

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET isTrash = :isTrash WHERE id = :id")
    suspend fun updateTrashStatus(id: Long, isTrash: Boolean)

    @Query("UPDATE media_items SET isTrash = :isTrash WHERE id IN (:ids)")
    suspend fun updateTrashStatusBatch(ids: List<Long>, isTrash: Boolean)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun updateFavoriteStatusBatch(ids: List<Long>, isFavorite: Boolean)

    @Query("UPDATE media_items SET albumName = :albumName WHERE id IN (:ids)")
    suspend fun updateAlbumBatch(ids: List<Long>, albumName: String)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deletePermanentlyBatch(ids: List<Long>)

    @Query("DELETE FROM media_items WHERE isTrash = 1")
    suspend fun emptyTrash()
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY isSystem DESC, createdAt ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE id = :id AND isSystem = 0")
    suspend fun deleteAlbum(id: Long)

    @Query("UPDATE albums SET name = :newName WHERE id = :id")
    suspend fun renameAlbum(id: Long, newName: String)
}
