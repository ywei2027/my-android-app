package com.example.myandroidapp.ui.calculator

sealed interface CalculatorOutputEvent {
    data class ExpandHistoryAndScroll(val itemId: String) : CalculatorOutputEvent
}
