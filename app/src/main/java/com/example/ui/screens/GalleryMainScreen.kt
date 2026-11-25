package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.components.AddToAlbumDialog
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.MediaThumbnailItem
import com.example.viewmodel.GalleryNavTab
import com.example.viewmodel.GalleryViewModel
import com.example.viewmodel.TimelineGrouping
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryMainScreen(viewModel: GalleryViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeMedia by viewModel.activeMedia.collectAsStateWithLifecycle()
    val favoriteMedia by viewModel.favoriteMedia.collectAsStateWithLifecycle()
    val trashMedia by viewModel.trashMedia.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchActive by remember { mutableStateOf(false) }

    // System Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris)
        }
    }

    // Permission Launcher for Android Storage/Photos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.showMessage("Photos permission granted. Syncing photos...")
        }
    }

    // Handle ViewModel snackbar feedback messages
    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    // Filter media items by search query and tag filter
    val filteredMedia = remember(activeMedia, uiState.searchQuery, uiState.selectedTagFilter) {
        activeMedia.filter { item ->
            val matchesSearch = if (uiState.searchQuery.isBlank()) true else {
                item.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.caption.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.albumName.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.tags.any { it.contains(uiState.searchQuery, ignoreCase = true) }
            }
            val matchesTag = if (uiState.selectedTagFilter == null) true else {
                item.tags.any { it.equals(uiState.selectedTagFilter, ignoreCase = true) } ||
                        item.albumName.equals(uiState.selectedTagFilter, ignoreCase = true)
            }
            matchesSearch && matchesTag
        }
    }

    // Available tags for the chips row
    val allTags = remember(activeMedia) {
        val set = mutableSetOf<String>()
        activeMedia.forEach { item ->
            set.addAll(item.tags)
            set.add(item.albumName)
        }
        set.toList().filter { it.isNotBlank() }
    }

    // Grouping for Timeline
    val timelineGroups = remember(filteredMedia, uiState.timelineGrouping) {
        groupMediaByTimeline(filteredMedia, uiState.timelineGrouping)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                // Batch Selection Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                            Text(
                                text = "${uiState.selectedMediaIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row {
                            TextButton(
                                onClick = {
                                    val allIds = filteredMedia.map { it.id }
                                    viewModel.selectAll(allIds)
                                }
                            ) {
                                Text("Select All")
                            }
                            IconButton(onClick = { viewModel.toggleFavoriteSelected() }) {
                                Icon(Icons.Default.Favorite, contentDescription = "Favorite")
                            }
                            IconButton(onClick = { viewModel.setShowAddToAlbumDialog(true) }) {
                                Icon(Icons.Default.Folder, contentDescription = "Add to Album")
                            }
                            IconButton(onClick = { viewModel.moveSelectedToTrash() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search photos, tags, albums...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                }
                            )
                        } else {
                            Column {
                                Text(
                                    text = when (uiState.navTab) {
                                        GalleryNavTab.PHOTOS -> "Photos"
                                        GalleryNavTab.ALBUMS -> "Albums"
                                        GalleryNavTab.FAVORITES -> "Favorites"
                                        GalleryNavTab.TRASH -> "Trash Bin"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.navTab == GalleryNavTab.PHOTOS) {
                                    Text(
                                        text = "${filteredMedia.size} items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        if (uiState.navTab == GalleryNavTab.PHOTOS) {
                            // Column count switch (2, 3, 4 columns)
                            IconButton(onClick = { viewModel.toggleGridColumns() }) {
                                Icon(
                                    imageVector = when (uiState.gridColumns) {
                                        2 -> Icons.Default.ViewAgenda
                                        3 -> Icons.Default.GridView
                                        else -> Icons.Default.ViewModule
                                    },
                                    contentDescription = "Grid Layout (${uiState.gridColumns} cols)"
                                )
                            }

                            // Import Photos FAB / Button
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add Photos",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = uiState.navTab == GalleryNavTab.PHOTOS,
                    onClick = { viewModel.setNavTab(GalleryNavTab.PHOTOS) },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Photos") },
                    label = { Text("Photos") },
                    modifier = Modifier.testTag("tab_photos")
                )
                NavigationBarItem(
                    selected = uiState.navTab == GalleryNavTab.ALBUMS,
                    onClick = { viewModel.setNavTab(GalleryNavTab.ALBUMS) },
                    icon = {
                        BadgedBox(badge = {
                            if (albums.isNotEmpty()) {
                                Badge { Text("${albums.size}") }
                            }
                        }) {
                            Icon(Icons.Default.Collections, contentDescription = "Albums")
                        }
                    },
                    label = { Text("Albums") },
                    modifier = Modifier.testTag("tab_albums")
                )
                NavigationBarItem(
                    selected = uiState.navTab == GalleryNavTab.FAVORITES,
                    onClick = { viewModel.setNavTab(GalleryNavTab.FAVORITES) },
                    icon = {
                        BadgedBox(badge = {
                            if (favoriteMedia.isNotEmpty()) {
                                Badge { Text("${favoriteMedia.size}") }
                            }
                        }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                        }
                    },
                    label = { Text("Favorites") },
                    modifier = Modifier.testTag("tab_favorites")
                )
                NavigationBarItem(
                    selected = uiState.navTab == GalleryNavTab.TRASH,
                    onClick = { viewModel.setNavTab(GalleryNavTab.TRASH) },
                    icon = {
                        BadgedBox(badge = {
                            if (trashMedia.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("${trashMedia.size}")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash")
                        }
                    },
                    label = { Text("Trash") },
                    modifier = Modifier.testTag("tab_trash")
                )
            }
        },
        floatingActionButton = {
            if (uiState.navTab == GalleryNavTab.PHOTOS && !uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_photos")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Photos")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.navTab) {
                GalleryNavTab.PHOTOS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Highlights & Memories Carousel (shown if not searching)
                        if (uiState.searchQuery.isBlank() && activeMedia.isNotEmpty()) {
                            MemoriesHighlightsCarousel(
                                mediaList = activeMedia,
                                onPhotoClick = { item ->
                                    val index = activeMedia.indexOfFirst { it.id == item.id }
                                    if (index >= 0) viewModel.openMediaDetail(index)
                                }
                            )
                        }

                        // Tags & Timeline Grouping Filter Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "All" Chip
                            FilterChip(
                                selected = uiState.selectedTagFilter == null,
                                onClick = { viewModel.setTagFilter(null) },
                                label = { Text("All Photos") }
                            )

                            // Grouping Chip Toggle
                            FilterChip(
                                selected = uiState.timelineGrouping != TimelineGrouping.ALL,
                                onClick = {
                                    val next = when (uiState.timelineGrouping) {
                                        TimelineGrouping.DAYS -> TimelineGrouping.MONTHS
                                        TimelineGrouping.MONTHS -> TimelineGrouping.ALL
                                        TimelineGrouping.ALL -> TimelineGrouping.DAYS
                                    }
                                    viewModel.setTimelineGrouping(next)
                                },
                                label = {
                                    Text(
                                        when (uiState.timelineGrouping) {
                                            TimelineGrouping.DAYS -> "By Day"
                                            TimelineGrouping.MONTHS -> "By Month"
                                            TimelineGrouping.ALL -> "No Grouping"
                                        }
                                    )
                                }
                            )

                            // Individual Tag Chips
                            allTags.forEach { tag ->
                                FilterChip(
                                    selected = uiState.selectedTagFilter == tag,
                                    onClick = {
                                        if (uiState.selectedTagFilter == tag) {
                                            viewModel.setTagFilter(null)
                                        } else {
                                            viewModel.setTagFilter(tag)
                                        }
                                    },
                                    label = { Text("#$tag") }
                                )
                            }
                        }

                        // Photos Grid
                        if (filteredMedia.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No photos found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import Photos")
                                    }
                                }
                            }
                        } else if (uiState.timelineGrouping == TimelineGrouping.ALL) {
                            // Flat Grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(uiState.gridColumns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                items(filteredMedia, key = { it.id }) { item ->
                                    MediaThumbnailItem(
                                        item = item,
                                        isSelected = uiState.selectedMediaIds.contains(item.id),
                                        isSelectionMode = uiState.isSelectionMode,
                                        onItemClick = {
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleItemSelection(item.id)
                                            } else {
                                                val index = filteredMedia.indexOfFirst { it.id == item.id }
                                                viewModel.openMediaDetail(index)
                                            }
                                        },
                                        onItemLongClick = { viewModel.toggleItemSelection(item.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                                    )
                                }
                            }
                        } else {
                            // Timeline Grouped LazyColumn
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                timelineGroups.forEach { group ->
                                    item(key = group.header) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = group.header,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${group.items.size} photos",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    item(key = "grid_${group.header}") {
                                        // Chunk into rows according to grid columns
                                        val rows = group.items.chunked(uiState.gridColumns)
                                        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                                            rows.forEach { rowItems ->
                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                    rowItems.forEach { item ->
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            MediaThumbnailItem(
                                                                item = item,
                                                                isSelected = uiState.selectedMediaIds.contains(item.id),
                                                                isSelectionMode = uiState.isSelectionMode,
                                                                onItemClick = {
                                                                    if (uiState.isSelectionMode) {
                                                                        viewModel.toggleItemSelection(item.id)
                                                                    } else {
                                                                        val index = filteredMedia.indexOfFirst { it.id == item.id }
                                                                        viewModel.openMediaDetail(index)
                                                                    }
                                                                },
                                                                onItemLongClick = { viewModel.toggleItemSelection(item.id) },
                                                                onToggleFavorite = { viewModel.toggleFavorite(item) }
                                                            )
                                                        }
                                                    }
                                                    // Fill remaining spaces in row for uniform width
                                                    val emptySlots = uiState.gridColumns - rowItems.size
                                                    if (emptySlots > 0) {
                                                        repeat(emptySlots) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GalleryNavTab.ALBUMS -> {
                    AlbumsScreen(
                        albums = albums,
                        selectedAlbumName = uiState.selectedAlbumName,
                        allMedia = activeMedia,
                        onSelectAlbum = { viewModel.selectAlbumForView(it) },
                        onCreateAlbumClick = { viewModel.setShowCreateAlbumDialog(true) },
                        onPhotoClick = { item ->
                            val index = activeMedia.indexOfFirst { it.id == item.id }
                            if (index >= 0) viewModel.openMediaDetail(index)
                        },
                        isSelectionMode = uiState.isSelectionMode,
                        selectedIds = uiState.selectedMediaIds,
                        onToggleSelection = { viewModel.toggleItemSelection(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                GalleryNavTab.FAVORITES -> {
                    FavoritesScreen(
                        favorites = favoriteMedia,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedIds = uiState.selectedMediaIds,
                        onToggleSelection = { viewModel.toggleItemSelection(it) },
                        onPhotoClick = { item ->
                            val index = favoriteMedia.indexOfFirst { it.id == item.id }
                            if (index >= 0) viewModel.openMediaDetail(index)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                GalleryNavTab.TRASH -> {
                    TrashScreen(
                        trashItems = trashMedia,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedIds = uiState.selectedMediaIds,
                        onToggleSelection = { viewModel.toggleItemSelection(it) },
                        onRestoreSelected = { viewModel.restoreSelectedFromTrash() },
                        onDeleteSelectedPermanently = { viewModel.deleteSelectedPermanently() },
                        onEmptyTrash = { viewModel.emptyTrash() }
                    )
                }
            }
        }
    }

    // Fullscreen Media Detail Viewer
    if (uiState.isViewingFullscreen) {
        val currentList = when (uiState.navTab) {
            GalleryNavTab.FAVORITES -> favoriteMedia
            GalleryNavTab.ALBUMS -> {
                if (uiState.selectedAlbumName != null) {
                    activeMedia.filter { it.albumName.equals(uiState.selectedAlbumName, ignoreCase = true) }
                } else activeMedia
            }
            else -> filteredMedia
        }

        MediaDetailScreen(
            mediaList = currentList,
            initialIndex = uiState.viewingMediaIndex,
            onClose = { viewModel.closeMediaDetail() },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onMoveToTrash = { viewModel.moveToTrash(it) },
            onOpenEditor = { viewModel.openPhotoEditor(it) },
            onSaveDetails = { item, caption, tags ->
                viewModel.updateMediaMetadata(item, caption, tags)
            }
        )
    }

    // Interactive Photo Editor Screen
    if (uiState.isEditingPhoto && uiState.editingItem != null) {
        PhotoEditorScreen(
            item = uiState.editingItem!!,
            onClose = { viewModel.closePhotoEditor() },
            onSave = { edited, saveAsCopy ->
                viewModel.saveEditedPhoto(edited, saveAsCopy)
            }
        )
    }

    // Dialogs
    if (uiState.showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { viewModel.setShowCreateAlbumDialog(false) },
            onConfirm = { name -> viewModel.createAlbum(name) }
        )
    }

    if (uiState.showAddToAlbumDialog) {
        AddToAlbumDialog(
            albums = albums,
            selectedCount = uiState.selectedMediaIds.size,
            onDismiss = { viewModel.setShowAddToAlbumDialog(false) },
            onAlbumSelected = { albumName ->
                viewModel.moveSelectedToAlbum(albumName)
            },
            onCreateNewAlbum = {
                viewModel.setShowAddToAlbumDialog(false)
                viewModel.setShowCreateAlbumDialog(true)
            }
        )
    }
}

@Composable
fun MemoriesHighlightsCarousel(
    mediaList: List<MediaItem>,
    onPhotoClick: (MediaItem) -> Unit
) {
    val highlightPhotos = remember(mediaList) {
        mediaList.take(6)
    }

    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Recent Highlights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(highlightPhotos, key = { "memory_${it.id}" }) { item ->
                Card(
                    modifier = Modifier
                        .size(width = 130.dp, height = 175.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPhotoClick(item) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uriString)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient bottom scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                        startY = 100f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.albumName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TimelineGroup(
    val header: String,
    val items: List<MediaItem>
)

private fun groupMediaByTimeline(
    items: List<MediaItem>,
    grouping: TimelineGrouping
): List<TimelineGroup> {
    if (grouping == TimelineGrouping.ALL) {
        return listOf(TimelineGroup("All Photos", items))
    }

    val now = System.currentTimeMillis()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - 86400000L
    val weekStart = todayStart - 86400000L * 7

    val dayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val groupedMap = LinkedHashMap<String, MutableList<MediaItem>>()

    items.forEach { item ->
        val header = when (grouping) {
            TimelineGrouping.DAYS -> {
                when {
                    item.dateTaken >= todayStart -> "Today"
                    item.dateTaken >= yesterdayStart -> "Yesterday"
                    item.dateTaken >= weekStart -> "Earlier this Week"
                    else -> dayFormat.format(Date(item.dateTaken))
                }
            }
            TimelineGrouping.MONTHS -> {
                monthFormat.format(Date(item.dateTaken))
            }
            else -> "Photos"
        }
        groupedMap.getOrPut(header) { mutableListOf() }.add(item)
    }

    return groupedMap.map { TimelineGroup(it.key, it.value) }
}
