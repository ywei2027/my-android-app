package com.example.myandroidapp.di

import com.example.myandroidapp.data.CalculatorHistoryDataSource
import com.example.myandroidapp.data.DataStoreCalculatorHistorySource
import com.example.myandroidapp.data.search.EncryptedSearchHistoryStore
import com.example.myandroidapp.data.search.InMemorySearchHistoryStore
import com.example.myandroidapp.data.search.SearchRepositoryImpl
import com.example.myandroidapp.domain.search.CalculatorHistorySearchSource
import com.example.myandroidapp.domain.search.SearchHistoryStore
import com.example.myandroidapp.domain.search.SearchRepository
import com.example.myandroidapp.domain.search.SearchSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    @IntoSet
    abstract fun bindCalculatorHistorySearchSource(
        impl: CalculatorHistorySearchSource,
    ): SearchSource

    @Binds
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl,
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryStore(
        impl: EncryptedSearchHistoryStore,
    ): SearchHistoryStore
}
