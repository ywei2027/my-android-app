package com.example.myandroidapp.data

import com.example.myandroidapp.data.model.CalcHistory
import kotlinx.coroutines.flow.Flow

interface CalculatorHistoryDataSource {
    val allHistory: Flow<List<CalcHistory>>
    suspend fun add(calcHistory: CalcHistory)
    suspend fun remove(id: String)
    suspend fun clearAll()
}
