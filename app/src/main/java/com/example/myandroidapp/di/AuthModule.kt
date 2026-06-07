package com.example.myandroidapp.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.myandroidapp.data.local.LoginStateManager
import com.example.myandroidapp.data.remote.LoginApi
import com.example.myandroidapp.data.remote.MockAuthInterceptor
import com.example.myandroidapp.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideLoginApi(@ApplicationContext context: Context): LoginApi {
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val client = if (isDebug) {
            baseClient.newBuilder().addInterceptor(MockAuthInterceptor()).build()
        } else {
            baseClient
        }

        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoginApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        loginApi: LoginApi,
        stateManager: LoginStateManager
    ): AuthRepository = AuthRepository(loginApi, stateManager)
}
