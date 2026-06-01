package com.example.myandroidapp.ui.calculator

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.data.CalculatorHistoryDataSource
import com.example.myandroidapp.ui.search.SearchScreen
import com.example.myandroidapp.ui.search.SearchViewModel

@Composable
fun CalculatorScreen(
    calculatorViewModel: CalculatorViewModel,
    searchViewModel: SearchViewModel,
    historyDataSource: CalculatorHistoryDataSource,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by calculatorViewModel.uiState.collectAsState()
    val outputEvents by calculatorViewModel.outputEvents.collectAsState(initial = null)
    val historyFlow = remember { historyDataSource.allHistory }

    var isSearchExpanded by remember { mutableStateOf(false) }
    val historyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Handle CalculatorOutputEvent from shared flow
    LaunchedEffect(outputEvents) {
        when (val event = outputEvents) {
            is CalculatorOutputEvent.ExpandHistoryAndScroll -> {
                calculatorViewModel.onEvent(CalculatorEvent.ToggleHistory)
                historyListState.animateScrollToItem(
                    historyListState.layoutInfo.totalItemsCount - 1,
                )
            }
            else -> {}
        }
    }

    // BackHandler: search expanded takes priority
    BackHandler(enabled = !isSearchExpanded && uiState.isHistoryExpanded) {
        calculatorViewModel.onEvent(CalculatorEvent.ToggleHistory)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                // Logout button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "退出",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable { onLogout() }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Docked SearchBar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "搜索计算历史，双击展开搜索" }
                        .clickable { isSearchExpanded = true },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "搜索计算历史",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = uiState.expression,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.result,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // History toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "历史记录",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { calculatorViewModel.onEvent(CalculatorEvent.ToggleHistory) },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Keypad (hidden when search expanded)
                AnimatedVisibility(visible = !isSearchExpanded) {
                    CalculatorKeypad(
                        onDigit = { calculatorViewModel.onEvent(CalculatorEvent.Digit(it)) },
                        onOperator = { calculatorViewModel.onEvent(CalculatorEvent.Operator(it)) },
                        onEquals = { calculatorViewModel.onEvent(CalculatorEvent.Equals) },
                        onClear = { calculatorViewModel.onEvent(CalculatorEvent.Clear) },
                        onDelete = { calculatorViewModel.onEvent(CalculatorEvent.Delete) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // History sheet (hoisted listState)
        CalculatorHistorySheet(
            historyFlow = historyFlow,
            isExpanded = uiState.isHistoryExpanded && !isSearchExpanded,
            highlightItemId = uiState.highlightItemId,
            onDismiss = { calculatorViewModel.onEvent(CalculatorEvent.ToggleHistory) },
            onItemClick = { calculatorViewModel.onEvent(CalculatorEvent.HistoryItemClick(it)) },
            onClearAll = { calculatorViewModel.onEvent(CalculatorEvent.ClearHistory) },
        )

        // Search overlay (AnimatedVisibility)
        SearchScreen(
            isExpanded = isSearchExpanded,
            viewModel = searchViewModel,
            onNavigateToCalculator = { itemId ->
                isSearchExpanded = false
                calculatorViewModel.onOutputEvent(CalculatorOutputEvent.ExpandHistoryAndScroll(itemId))
            },
            onDismiss = {
                focusManager.clearFocus()
                isSearchExpanded = false
            },
        )
    }
}

@Composable
private fun CalculatorKeypad(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1: C () % ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyButton("C", Modifier.weight(1f)) { onClear() }
            KeyButton("()", Modifier.weight(1f)) { /* future feature */ }
            KeyButton("%", Modifier.weight(1f)) { onOperator("%") }
            KeyButton("÷", Modifier.weight(1f), isOperator = true) { onOperator("÷") }
        }
        // Row 2: 7 8 9 ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyButton("7", Modifier.weight(1f)) { onDigit("7") }
            KeyButton("8", Modifier.weight(1f)) { onDigit("8") }
            KeyButton("9", Modifier.weight(1f)) { onDigit("9") }
            KeyButton("×", Modifier.weight(1f), isOperator = true) { onOperator("×") }
        }
        // Row 3: 4 5 6 -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyButton("4", Modifier.weight(1f)) { onDigit("4") }
            KeyButton("5", Modifier.weight(1f)) { onDigit("5") }
            KeyButton("6", Modifier.weight(1f)) { onDigit("6") }
            KeyButton("-", Modifier.weight(1f), isOperator = true) { onOperator("-") }
        }
        // Row 4: 1 2 3 +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyButton("1", Modifier.weight(1f)) { onDigit("1") }
            KeyButton("2", Modifier.weight(1f)) { onDigit("2") }
            KeyButton("3", Modifier.weight(1f)) { onDigit("3") }
            KeyButton("+", Modifier.weight(1f), isOperator = true) { onOperator("+") }
        }
        // Row 5: 0 (span 2) . =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyButton("0", Modifier.weight(2f)) { onDigit("0") }
            KeyButton(".", Modifier.weight(1f)) { onDigit(".") }
            KeyButton("=", Modifier.weight(1f), isOperator = true) { onEquals() }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = if (isOperator) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
