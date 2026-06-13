package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.edit
import com.example.data.dataStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsManagerTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun test_defaultThemeIsDark() = runBlocking {
        val isDark = settingsManager.isDarkThemeFlow.first()
        assertFalse(isDark)
    }

    @Test
    fun test_setDarkThemeFalse() = runBlocking {
        settingsManager.setDarkTheme(false)
        val isDark = settingsManager.isDarkThemeFlow.first()
        assertFalse(isDark)
    }

    @Test
    fun test_setDarkThemeTrue() = runBlocking {
        settingsManager.setDarkTheme(false)
        settingsManager.setDarkTheme(true)
        val isDark = settingsManager.isDarkThemeFlow.first()
        assertTrue(isDark)
    }

    @Test
    fun test_defaultNotificationsEnabled() = runBlocking {
        val enabled = settingsManager.notificationsEnabledFlow.first()
        assertTrue(enabled)
    }

    @Test
    fun test_setNotificationsEnabledFalse() = runBlocking {
        settingsManager.setNotificationsEnabled(false)
        val enabled = settingsManager.notificationsEnabledFlow.first()
        assertFalse(enabled)
    }

    @Test
    fun test_setNotificationsEnabledTrue() = runBlocking {
        settingsManager.setNotificationsEnabled(false)
        settingsManager.setNotificationsEnabled(true)
        val enabled = settingsManager.notificationsEnabledFlow.first()
        assertTrue(enabled)
    }

    @Test
    fun test_defaultAccentColorIsNull() = runBlocking {
        val color = settingsManager.accentColorFlow.first()
        assertNull(color)
    }

    @Test
    fun test_setAccentColor() = runBlocking {
        val testColor: Long = 0xFF2196F3
        settingsManager.setAccentColor(testColor)
        val color = settingsManager.accentColorFlow.first()
        assertEquals(testColor, color)
    }

    @Test
    fun test_setMultipleSettings() = runBlocking {
        settingsManager.setDarkTheme(false)
        settingsManager.setNotificationsEnabled(false)
        settingsManager.setAccentColor(0xFFFF9800)
        
        val isDark = settingsManager.isDarkThemeFlow.first()
        val enabled = settingsManager.notificationsEnabledFlow.first()
        val color = settingsManager.accentColorFlow.first()

        assertFalse(isDark)
        assertFalse(enabled)
        assertEquals(0xFFFF9800, color)
    }
}
