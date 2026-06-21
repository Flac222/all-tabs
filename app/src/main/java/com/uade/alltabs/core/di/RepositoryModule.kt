package com.uade.alltabs.core.di

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.data.repository.TabRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTabRepository(tabRepositoryImpl: TabRepositoryImpl): TabRepository
}


