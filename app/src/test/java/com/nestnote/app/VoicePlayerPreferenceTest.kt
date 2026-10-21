package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoicePlayerPreferenceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun compactPlayerIsTheDefaultWhenPreferenceHasNotBeenSaved() {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        assertTrue(preferences.getBoolean(PREF_VOICE_PLAYER_COMPACT, true))
    }

    @Test
    fun compactAndFullPlayerChoicePersistsAcrossPreferenceReads() {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        preferences.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, false).commit()
        assertFalse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(PREF_VOICE_PLAYER_COMPACT, true))

        preferences.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, true).commit()
        assertTrue(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(PREF_VOICE_PLAYER_COMPACT, false))
    }

    @Test
    fun unrelatedPreferencesArePreservedWhenPlayerStyleChanges() {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        preferences.edit().putString(PREF_THEME, ThemeMode.DARK.name).commit()

        preferences.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, false).commit()

        assertFalse(preferences.getBoolean(PREF_VOICE_PLAYER_COMPACT, true))
        assertTrue(preferences.getString(PREF_THEME, null) == ThemeMode.DARK.name)
    }
}
