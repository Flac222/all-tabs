package com.uade.alltabs.core.di

import android.app.Application
import androidx.room.Room
import com.uade.alltabs.data.local.AllTabsDatabase
import com.uade.alltabs.data.local.FavoriteDao
import com.uade.alltabs.data.local.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAllTabsDatabase(app: Application): AllTabsDatabase {
        return Room.databaseBuilder(
            app,
            AllTabsDatabase::class.java,
            AllTabsDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTabDao(db: AllTabsDatabase): TabDao {
        return db.tabDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(db: AllTabsDatabase): FavoriteDao {
        return db.favoriteDao()
    }
}
