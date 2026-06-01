package com.example.myandroidapp.data.search

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.myandroidapp.domain.search.SearchHistoryStore
import com.example.myandroidapp.domain.search.SearchQuery
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import java.security.KeyStoreException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedSearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchHistoryStore {

    private val gson = Gson()
    private val mutex = Mutex()
    private val historyKey = "search_queries"

    private val prefs by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "search_history_store",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: Flow<String> = _errors

    private val _history = MutableStateFlow<List<SearchQuery>>(emptyList())

    init {
        _history.value = loadFromStore()
    }

    override val history: Flow<List<SearchQuery>> = _history.asStateFlow()

    override suspend fun addQuery(query: String) = mutex.withLock {
        val trimmed = query.trim()
        val normalized = trimmed.lowercase()
        val current = _history.value.toMutableList()
        val existingIdx = current.indexOfFirst {
            it.query.trim().lowercase() == normalized
        }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
        }
        current.add(0, SearchQuery(query = trimmed, timestamp = System.currentTimeMillis()))
        val updated = current.take(5)
        _history.value = updated
        saveToStore(updated)
    }

    override suspend fun removeQuery(query: String) = mutex.withLock {
        val current = _history.value.toMutableList()
        current.removeAll { it.query == query }
        _history.value = current
        saveToStore(current)
    }

    override suspend fun clearAll() = mutex.withLock {
        _history.value = emptyList()
        saveToStore(emptyList())
    }

    private fun loadFromStore(): List<SearchQuery> {
        return try {
            val json = prefs.getString(historyKey, null) ?: return emptyList()
            val type = object : TypeToken<List<SearchQuery>>() {}.type
            gson.fromJson<List<SearchQuery>>(json, type) ?: emptyList()
        } catch (e: KeyStoreException) {
            Timber.w(e, "Keystore unavailable, history degraded")
            emptyList()
        } catch (e: IOException) {
            Timber.e(e, "History read/write failed")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error loading search history")
            emptyList()
        }
    }

    private fun saveToStore(history: List<SearchQuery>) {
        try {
            val json = gson.toJson(history)
            prefs.edit().putString(historyKey, json).apply()
        } catch (e: KeyStoreException) {
            Timber.w(e, "Keystore unavailable, history save degraded")
        } catch (e: IOException) {
            Timber.e(e, "History write failed")
        }
    }
}
