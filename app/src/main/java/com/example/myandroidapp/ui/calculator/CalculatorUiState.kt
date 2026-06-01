package com.example.myandroidapp.ui.calculator

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "0",
    val isHistoryExpanded: Boolean = false,
    val highlightItemId: String? = null,
)
