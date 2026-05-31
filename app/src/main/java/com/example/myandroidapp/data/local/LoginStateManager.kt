package com.example.myandroidapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Top-level DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login_state")

/**
 * 登录状态持久化管理器。
 * 使用 DataStore 存储 token、手机号、登录时间等信息。
 * 用户关闭 App 后重新打开，可以直接判断是否已登录。
 */
@Singleton
class LoginStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
        val KEY_LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")
    }

    /**
     * 是否已登录的 Flow，UI 层可直接 collect。
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    /**
     * 当前认证 token。
     */
    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]
    }

    /**
     * 已登录用户的手机号。
     */
    val phoneNumber: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PHONE_NUMBER]
    }

    /**
     * 保存登录状态。
     * 登录成功后调用，持久化 token 和手机号。
     */
    suspend fun saveLoginState(
        token: String,
        phone: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_PHONE_NUMBER] = phone
            prefs[KEY_LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * 清除登录状态（退出登录）。
     */
    suspend fun clearLoginState() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_IS_LOGGED_IN)
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_PHONE_NUMBER)
            prefs.remove(KEY_LOGIN_TIMESTAMP)
        }
    }
}
