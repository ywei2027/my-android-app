package com.example.myandroidapp.ui.calculator

sealed interface CalculatorEvent {
    data class Digit(val value: String) : CalculatorEvent
    data class Operator(val value: String) : CalculatorEvent
    data object Equals : CalculatorEvent
    data object Clear : CalculatorEvent
    data object Delete : CalculatorEvent
    data object ToggleHistory : CalculatorEvent
    data class HistoryItemClick(val itemId: String) : CalculatorEvent
    data object ClearHistory : CalculatorEvent
}
