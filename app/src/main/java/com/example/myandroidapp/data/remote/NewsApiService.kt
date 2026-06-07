package com.example.myandroidapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "cn",
        @Query("category") category: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): NewsApiResponse
}
