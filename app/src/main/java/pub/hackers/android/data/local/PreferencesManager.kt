package pub.hackers.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pub.hackers.android.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
        private val CONFIRM_BEFORE_SHARE = booleanPreferencesKey("confirm_before_share")
        private val TIMELINE_MAX_LENGTH = intPreferencesKey("timeline_max_length")
        private val USE_IN_APP_BROWSER = booleanPreferencesKey("use_in_app_browser")
        private val FONT_SIZE_MULTIPLIER = intPreferencesKey("font_size_multiplier") // stored as percentage (100 = 1.0x)
        private val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SHOW_LANGUAGE_FILTER = booleanPreferencesKey("show_language_filter")
        private const val MAX_RECENT_SEARCHES = 10
    }

    val confirmBeforeDelete: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[CONFIRM_BEFORE_DELETE] ?: true
    }

    val confirmBeforeShare: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[CONFIRM_BEFORE_SHARE] ?: false
    }

    val timelineMaxLength: Flow<Int> = context.preferencesDataStore.data.map { prefs ->
        prefs[TIMELINE_MAX_LENGTH] ?: 0 // 0 = unlimited
    }

    val useInAppBrowser: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[USE_IN_APP_BROWSER] ?: true
    }

    suspend fun setUseInAppBrowser(value: Boolean) {
        context.preferencesDataStore.edit { prefs ->
            prefs[USE_IN_APP_BROWSER] = value
        }
    }

    val fontSizePercent: Flow<Int> = context.preferencesDataStore.data.map { prefs ->
        prefs[FONT_SIZE_MULTIPLIER] ?: 100
    }

    suspend fun setFontSizePercent(value: Int) {
        context.preferencesDataStore.edit { prefs ->
            prefs[FONT_SIZE_MULTIPLIER] = value.coerceIn(75, 200)
        }
    }

    val themeMode: Flow<ThemeMode> = context.preferencesDataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[THEME_MODE])
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.preferencesDataStore.edit { prefs ->
            prefs[THEME_MODE] = value.storageValue
        }
    }

    val showLanguageFilter: Flow<Boolean> = context.preferencesDataStore.data.map { prefs ->
        prefs[SHOW_LANGUAGE_FILTER] ?: false
    }

    suspend fun setShowLanguageFilter(value: Boolean) {
        context.preferencesDataStore.edit { prefs ->
            prefs[SHOW_LANGUAGE_FILTER] = value
        }
    }

    suspend fun setConfirmBeforeDelete(value: Boolean) {
        context.preferencesDataStore.edit { prefs ->
            prefs[CONFIRM_BEFORE_DELETE] = value
        }
    }

    suspend fun setConfirmBeforeShare(value: Boolean) {
        context.preferencesDataStore.edit { prefs ->
            prefs[CONFIRM_BEFORE_SHARE] = value
        }
    }

    suspend fun setTimelineMaxLength(value: Int) {
        context.preferencesDataStore.edit { prefs ->
            prefs[TIMELINE_MAX_LENGTH] = value
        }
    }

    val recentSearches: Flow<List<String>> = context.preferencesDataStore.data.map { prefs ->
        val raw = prefs[RECENT_SEARCHES] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split("\u0000")
    }

    suspend fun addRecentSearch(query: String) {
        context.preferencesDataStore.edit { prefs ->
            val raw = prefs[RECENT_SEARCHES] ?: ""
            val existing = if (raw.isEmpty()) mutableListOf() else raw.split("\u0000").toMutableList()
            existing.remove(query)
            existing.add(0, query)
            val trimmed = existing.take(MAX_RECENT_SEARCHES)
            prefs[RECENT_SEARCHES] = trimmed.joinToString("\u0000")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.preferencesDataStore.edit { prefs ->
            val raw = prefs[RECENT_SEARCHES] ?: ""
            val existing = if (raw.isEmpty()) mutableListOf() else raw.split("\u0000").toMutableList()
            existing.remove(query)
            prefs[RECENT_SEARCHES] = existing.joinToString("\u0000")
        }
    }

    suspend fun clearRecentSearches() {
        context.preferencesDataStore.edit { prefs ->
            prefs[RECENT_SEARCHES] = ""
        }
    }
}
