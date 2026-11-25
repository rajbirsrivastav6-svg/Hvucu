package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.viewmodel.TimelineGrouping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_settings")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class GallerySettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultGridColumns: Int = 3,
    val defaultTimelineGrouping: TimelineGrouping = TimelineGrouping.DAYS,
    val showHighlights: Boolean = true,
    val showThumbnailBadges: Boolean = true,
    val slideshowSpeedSeconds: Int = 3,
    val confirmBeforeDelete: Boolean = true
)

class GalleryPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_GRID_COLUMNS = intPreferencesKey("default_grid_columns")
        val DEFAULT_TIMELINE_GROUPING = stringPreferencesKey("default_timeline_grouping")
        val SHOW_HIGHLIGHTS = booleanPreferencesKey("show_highlights")
        val SHOW_THUMBNAIL_BADGES = booleanPreferencesKey("show_thumbnail_badges")
        val SLIDESHOW_SPEED_SECONDS = intPreferencesKey("slideshow_speed_seconds")
        val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
    }

    val settingsFlow: Flow<GallerySettings> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM)

        val groupingName = prefs[Keys.DEFAULT_TIMELINE_GROUPING] ?: TimelineGrouping.DAYS.name
        val grouping = runCatching { TimelineGrouping.valueOf(groupingName) }.getOrDefault(TimelineGrouping.DAYS)

        val cols = (prefs[Keys.DEFAULT_GRID_COLUMNS] ?: 3).coerceIn(2, 4)
        val speed = (prefs[Keys.SLIDESHOW_SPEED_SECONDS] ?: 3).let {
            if (it in listOf(2, 3, 5)) it else 3
        }

        GallerySettings(
            themeMode = themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            defaultGridColumns = cols,
            defaultTimelineGrouping = grouping,
            showHighlights = prefs[Keys.SHOW_HIGHLIGHTS] ?: true,
            showThumbnailBadges = prefs[Keys.SHOW_THUMBNAIL_BADGES] ?: true,
            slideshowSpeedSeconds = speed,
            confirmBeforeDelete = prefs[Keys.CONFIRM_BEFORE_DELETE] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_GRID_COLUMNS] = columns.coerceIn(2, 4) }
    }

    suspend fun setDefaultTimelineGrouping(grouping: TimelineGrouping) {
        context.dataStore.edit { it[Keys.DEFAULT_TIMELINE_GROUPING] = grouping.name }
    }

    suspend fun setShowHighlights(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HIGHLIGHTS] = show }
    }

    suspend fun setShowThumbnailBadges(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_THUMBNAIL_BADGES] = show }
    }

    suspend fun setSlideshowSpeedSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.SLIDESHOW_SPEED_SECONDS] = seconds }
    }

    suspend fun setConfirmBeforeDelete(confirm: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_BEFORE_DELETE] = confirm }
    }
}
