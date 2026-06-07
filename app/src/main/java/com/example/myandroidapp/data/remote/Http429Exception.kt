package com.example.myandroidapp.data.remote

/**
 * Thrown when NewsAPI returns HTTP 429 Too Many Requests.
 */
class Http429Exception(message: String = "请求太频繁，请稍后再试") : Exception(message)
