package com.example.myandroidapp

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginRepository {
    fun login(username: String, password: String): Result<Boolean> {
        // Network call simulation
        Thread.sleep(2000)  // P0: Blocking call on main thread
        return Result.success(true)
    }
}

class LoginViewModel(private val activity: Activity) : ViewModel() {  // P0: Activity reference = memory leak

    private val repository = LoginRepository()

    fun login(username: String, password: String) {
        GlobalScope.launch {  // P1: Should use viewModelScope
            try {
                val result = repository.login(username, password)  // P1: View layer accessing Repository directly
                result.onSuccess {
                    activity.runOnUiThread {
                        // Success handling
                    }
                }
            } catch (e: Exception) {
                // P1: Empty catch block
            }
        }
    }

    fun validateInput(username: String?, password: String?): Boolean {
        // P2: Missing null safety, potential NPE
        return username!!.length > 3 && password!!.length > 6
    }

    private var cachedData: List<String>? = null

    fun getCachedData(): List<String> {
        // P0: Force unwrap nullable, will crash if null
        return cachedData!!
    }
}
