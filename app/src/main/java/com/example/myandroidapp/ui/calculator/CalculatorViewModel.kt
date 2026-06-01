package com.example.myandroidapp.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.CalculatorHistoryDataSource
import com.example.myandroidapp.data.model.CalcHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val historyDataSource: CalculatorHistoryDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _outputEvents = MutableSharedFlow<CalculatorOutputEvent>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val outputEvents: SharedFlow<CalculatorOutputEvent> = _outputEvents.asSharedFlow()

    private var currentOperand = ""
    private var currentOperator = ""
    private var previousResult = 0.0
    private var shouldResetOperand = false

    fun onEvent(event: CalculatorEvent) {
        when (event) {
            is CalculatorEvent.Digit -> onDigit(event.value)
            is CalculatorEvent.Operator -> onOperator(event.value)
            is CalculatorEvent.Equals -> onEquals()
            is CalculatorEvent.Clear -> onClear()
            is CalculatorEvent.Delete -> onDelete()
            is CalculatorEvent.ToggleHistory -> onToggleHistory()
            is CalculatorEvent.HistoryItemClick -> onHistoryItemClick(event.itemId)
            is CalculatorEvent.ClearHistory -> onClearHistory()
        }
    }

    fun onOutputEvent(event: CalculatorOutputEvent) {
        viewModelScope.launch {
            _outputEvents.emit(event)
        }
    }

    private fun onDigit(digit: String) {
        if (shouldResetOperand) {
            currentOperand = ""
            shouldResetOperand = false
        }
        currentOperand += digit
        updateExpression()
    }

    private fun onOperator(op: String) {
        if (currentOperand.isNotEmpty()) {
            if (currentOperator.isNotEmpty()) {
                onEquals()
            }
            previousResult = currentOperand.toDoubleOrNull() ?: 0.0
            currentOperator = op
            currentOperand = ""
            updateExpression()
        }
    }

    private fun onEquals() {
        if (currentOperator.isNotEmpty() && currentOperand.isNotEmpty()) {
            val operand = currentOperand.toDoubleOrNull() ?: return
            val result = when (currentOperator) {
                "+" -> previousResult + operand
                "-" -> previousResult - operand
                "×" -> previousResult * operand
                "÷" -> if (operand != 0.0) previousResult / operand else return
                else -> return
            }
            val expression = "${formatNumber(previousResult)} $currentOperator ${formatNumber(operand)}"
            _uiState.value = _uiState.value.copy(
                expression = expression,
                result = formatNumber(result),
            )
            saveHistory(expression, formatNumber(result))
            previousResult = result
            currentOperand = ""
            currentOperator = ""
            shouldResetOperand = true
        }
    }

    private fun onClear() {
        currentOperand = ""
        currentOperator = ""
        previousResult = 0.0
        shouldResetOperand = false
        _uiState.value = CalculatorUiState()
    }

    private fun onDelete() {
        if (currentOperand.isNotEmpty()) {
            currentOperand = currentOperand.dropLast(1)
            updateExpression()
        }
    }

    private fun onToggleHistory() {
        _uiState.value = _uiState.value.copy(
            isHistoryExpanded = !_uiState.value.isHistoryExpanded,
        )
    }

    private fun onHistoryItemClick(itemId: String) {
        // handled by CalculatorScreen via LaunchedEffect
        viewModelScope.launch {
            _outputEvents.emit(CalculatorOutputEvent.ExpandHistoryAndScroll(itemId))
        }
    }

    private fun onClearHistory() {
        viewModelScope.launch {
            historyDataSource.clearAll()
        }
    }

    private fun updateExpression() {
        val expr = if (currentOperator.isNotEmpty()) {
            "${formatNumber(previousResult)} $currentOperator $currentOperand"
        } else {
            currentOperand
        }
        _uiState.value = _uiState.value.copy(expression = expr)
    }

    private fun saveHistory(expression: String, result: String) {
        viewModelScope.launch {
            val history = CalcHistory(
                id = UUID.randomUUID().toString(),
                expression = expression,
                result = result,
                timestamp = System.currentTimeMillis(),
            )
            historyDataSource.add(history)
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}
