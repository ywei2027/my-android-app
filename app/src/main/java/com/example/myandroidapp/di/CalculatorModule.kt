package com.example.myandroidapp.di

import com.example.myandroidapp.data.CalculatorHistoryDataSource
import com.example.myandroidapp.data.DataStoreCalculatorHistorySource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalculatorModule {

    @Binds
    @Singleton
    abstract fun bindCalculatorHistoryDataSource(
        impl: DataStoreCalculatorHistorySource,
    ): CalculatorHistoryDataSource
}
