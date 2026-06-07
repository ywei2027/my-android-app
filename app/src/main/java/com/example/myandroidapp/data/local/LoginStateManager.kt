package com.example.myandroidapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myandroidapp.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login_state")

@Singleton
class LoginStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val KEY_USER_ID = stringPreferencesKey("user_id")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN] != null
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL]
    }

    val userDisplayName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_DISPLAY_NAME]
    }

    suspend fun saveUser(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_USER_EMAIL] = user.email
            prefs[KEY_USER_DISPLAY_NAME] = user.displayName
            prefs[KEY_USER_ID] = user.id
        }
    }

    fun getToken(): String? {
        return kotlinx.coroutines.runBlocking {
            context.dataStore.data.map { prefs -> prefs[KEY_AUTH_TOKEN] }.firstOrNull()
        }
    }

    fun getUser(): User? {
        return kotlinx.coroutines.runBlocking {
            context.dataStore.data.map { prefs ->
                val id = prefs[KEY_USER_ID]
                val email = prefs[KEY_USER_EMAIL]
                val displayName = prefs[KEY_USER_DISPLAY_NAME]
                if (id != null && email != null && displayName != null) {
                    User(id, email, displayName)
                } else {
                    null
                }
            }.firstOrNull()
        }
    }

    fun clear() {
        kotlinx.coroutines.runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }
}
