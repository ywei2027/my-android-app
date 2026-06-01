package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myandroidapp.data.CalculatorHistoryDataSource
import com.example.myandroidapp.ui.calculator.CalculatorScreen
import com.example.myandroidapp.ui.calculator.CalculatorViewModel
import com.example.myandroidapp.ui.login.LoginViewModel
import com.example.myandroidapp.ui.search.SearchViewModel
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent(
    calculatorViewModel: CalculatorViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val historyDataSource = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CalculatorHistoryDataSourceProvider::class.java,
        )
        entryPoint.calculatorHistoryDataSource()
    }

    CalculatorScreen(
        calculatorViewModel = calculatorViewModel,
        searchViewModel = searchViewModel,
        historyDataSource = historyDataSource,
        onLogout = { loginViewModel.onLogout() },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CalculatorHistoryDataSourceProvider {
    fun calculatorHistoryDataSource(): CalculatorHistoryDataSource
}
