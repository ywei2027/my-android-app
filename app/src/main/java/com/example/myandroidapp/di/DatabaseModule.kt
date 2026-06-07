package com.example.myandroidapp.di

import android.content.Context
import androidx.room.Room
import com.example.myandroidapp.data.local.NewsDao
import com.example.myandroidapp.data.local.NewsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase {
        return Room.databaseBuilder(context, NewsDatabase::class.java, "news.db")
            .fallbackToDestructiveMigration() // TODO: v1.1 实现 Migration(1,2) 替代破坏性迁移
            .build()
    }

    @Provides
    fun provideNewsDao(db: NewsDatabase): NewsDao = db.newsDao()
}
