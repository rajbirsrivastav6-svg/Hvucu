package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GalleryDatabase
import com.example.data.model.Album
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GalleryNavTab {
    PHOTOS,
    ALBUMS,
    FAVORITES,
    TRASH
}

enum class TimelineGrouping {
    ALL,
    DAYS,
    MONTHS
}

data class GalleryUiState(
    val navTab: GalleryNavTab = GalleryNavTab.PHOTOS,
    val timelineGrouping: TimelineGrouping = TimelineGrouping.DAYS,
    val gridColumns: Int = 3,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val searchQuery: String = "",
    val selectedTagFilter: String? = null,
    val viewingMediaIndex: Int = -1,
    val isViewingFullscreen: Boolean = false,
    val isEditingPhoto: Boolean = false,
    val editingItem: MediaItem? = null,
    val isSlideshowActive: Boolean = false,
    val selectedAlbumName: String? = null,
    val showExifInfoSheet: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val showAddToAlbumDialog: Boolean = false,
    val userFeedbackMessage: String? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GalleryDatabase.getDatabase(application)
    private val repository = GalleryRepository(application, database.mediaDao(), database.albumDao())

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    val activeMedia: StateFlow<List<MediaItem>> = repository.activeMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMedia: StateFlow<List<MediaItem>> = repository.favoriteMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashMedia: StateFlow<List<MediaItem>> = repository.trashMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
            repository.syncDeviceMedia()
        }
    }

    fun setNavTab(tab: GalleryNavTab) {
        _uiState.value = _uiState.value.copy(
            navTab = tab,
            selectedMediaIds = emptySet(),
            isSelectionMode = false,
            selectedAlbumName = null
        )
    }

    fun setTimelineGrouping(grouping: TimelineGrouping) {
        _uiState.value = _uiState.value.copy(timelineGrouping = grouping)
    }

    fun toggleGridColumns() {
        val next = when (_uiState.value.gridColumns) {
            2 -> 3
            3 -> 4
            else -> 2
        }
        _uiState.value = _uiState.value.copy(gridColumns = next)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setTagFilter(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTagFilter = tag)
    }

    fun toggleItemSelection(id: Long) {
        val current = _uiState.value.selectedMediaIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(
            selectedMediaIds = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun selectAll(allIds: List<Long>) {
        _uiState.value = _uiState.value.copy(
            selectedMediaIds = allIds.toSet(),
            isSelectionMode = true
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedMediaIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun openMediaDetail(index: Int) {
        _uiState.value = _uiState.value.copy(
            viewingMediaIndex = index,
            isViewingFullscreen = true
        )
    }

    fun closeMediaDetail() {
        _uiState.value = _uiState.value.copy(
            isViewingFullscreen = false,
            viewingMediaIndex = -1,
            isSlideshowActive = false
        )
    }

    fun setViewingIndex(index: Int) {
        _uiState.value = _uiState.value.copy(viewingMediaIndex = index)
    }

    fun openPhotoEditor(item: MediaItem) {
        _uiState.value = _uiState.value.copy(
            isEditingPhoto = true,
            editingItem = item
        )
    }

    fun closePhotoEditor() {
        _uiState.value = _uiState.value.copy(
            isEditingPhoto = false,
            editingItem = null
        )
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    fun toggleFavoriteSelected() {
        val ids = _uiState.value.selectedMediaIds.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.toggleFavoriteBatch(ids, true)
                clearSelection()
                showMessage("Added to favorites")
            }
        }
    }

    fun moveToTrash(item: MediaItem) {
        viewModelScope.launch {
            repository.moveToTrash(item.id)
            if (_uiState.value.isViewingFullscreen) {
                closeMediaDetail()
            }
            showMessage("Photo moved to Trash")
        }
    }

    fun moveSelectedToTrash() {
        val ids = _uiState.value.selectedMediaIds.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.moveToTrashBatch(ids)
                clearSelection()
                showMessage("${ids.size} photos moved to Trash")
            }
        }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
            showMessage("Photo restored")
        }
    }

    fun restoreSelectedFromTrash() {
        val ids = _uiState.value.selectedMediaIds.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.restoreFromTrashBatch(ids)
                clearSelection()
                showMessage("${ids.size} photos restored")
            }
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            repository.deletePermanently(id)
            showMessage("Photo permanently deleted")
        }
    }

    fun deleteSelectedPermanently() {
        val ids = _uiState.value.selectedMediaIds.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.deletePermanentlyBatch(ids)
                clearSelection()
                showMessage("${ids.size} photos permanently deleted")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            showMessage("Trash emptied")
        }
    }

    fun selectAlbumForView(albumName: String?) {
        _uiState.value = _uiState.value.copy(selectedAlbumName = albumName)
    }

    fun createAlbum(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.createAlbum(name.trim())
                _uiState.value = _uiState.value.copy(showCreateAlbumDialog = false)
                showMessage("Album '$name' created")
            }
        }
    }

    fun moveSelectedToAlbum(albumName: String) {
        val ids = _uiState.value.selectedMediaIds.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.moveItemsToAlbum(ids, albumName)
                clearSelection()
                _uiState.value = _uiState.value.copy(showAddToAlbumDialog = false)
                showMessage("${ids.size} photos moved to '$albumName'")
            }
        }
    }

    fun importMediaUris(uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEachIndexed { index, uri ->
                repository.importMedia(uri, name = "Imported_${System.currentTimeMillis()}_$index")
            }
            showMessage("${uris.size} photos added to Gallery")
        }
    }

    fun saveEditedPhoto(edited: MediaItem, saveAsCopy: Boolean) {
        viewModelScope.launch {
            if (saveAsCopy) {
                repository.saveEditedMediaAsNew(edited, "${edited.title}_Edited")
                showMessage("Saved as new photo")
            } else {
                repository.updateMedia(edited)
                showMessage("Photo updated")
            }
            closePhotoEditor()
        }
    }

    fun updateMediaMetadata(item: MediaItem, caption: String, tags: List<String>) {
        viewModelScope.launch {
            val updated = item.copy(caption = caption, tags = tags)
            repository.updateMedia(updated)
            showMessage("Details updated")
        }
    }

    fun toggleSlideshow(start: Boolean) {
        _uiState.value = _uiState.value.copy(isSlideshowActive = start)
    }

    fun setShowExifInfo(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExifInfoSheet = show)
    }

    fun setShowCreateAlbumDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateAlbumDialog = show)
    }

    fun setShowAddToAlbumDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddToAlbumDialog = show)
    }

    fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(userFeedbackMessage = msg)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(userFeedbackMessage = null)
    }
}
