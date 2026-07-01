package com.llamadroid.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(private val context: Context) {

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_KEY] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[ONBOARDING_KEY] = true }
    }

    companion object {
        private val ONBOARDING_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
