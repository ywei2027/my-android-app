package com.example.myandroidapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myandroidapp.data.model.CalcHistory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calculator_history")

@Singleton
class DataStoreCalculatorHistorySource @Inject constructor(
    @ApplicationContext private val context: Context,
) : CalculatorHistoryDataSource {

    private val gson = Gson()
    private val historyKey = stringPreferencesKey("calc_history_list")

    override val allHistory: Flow<List<CalcHistory>> = context.dataStore.data.map { prefs ->
        val json = prefs[historyKey] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<CalcHistory>>() {}.type
            gson.fromJson<List<CalcHistory>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun add(calcHistory: CalcHistory) {
        context.dataStore.edit { prefs ->
            val current = deserialize(prefs[historyKey])
            val updated = listOf(calcHistory) + current.take(19) // cap at 20
            prefs[historyKey] = gson.toJson(updated)
        }
    }

    override suspend fun remove(id: String) {
        context.dataStore.edit { prefs ->
            val current = deserialize(prefs[historyKey])
            val updated = current.filter { it.id != id }
            prefs[historyKey] = gson.toJson(updated)
        }
    }

    override suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.remove(historyKey)
        }
    }

    private fun deserialize(json: String?): List<CalcHistory> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<CalcHistory>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
