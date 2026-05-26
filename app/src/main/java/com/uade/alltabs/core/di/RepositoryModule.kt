package com.uade.alltabs.core.di

import com.uade.alltabs.domain.repository.TabRepository
import com.uade.alltabs.data.repository.TabRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.Provides
import com.uade.alltabs.domain.usecase.GetTabsByMbidUseCase

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTabRepository(tabRepositoryImpl: TabRepositoryImpl): TabRepository
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetTabsByMbidUseCase(tabRepository: TabRepository): GetTabsByMbidUseCase {
        return GetTabsByMbidUseCase(tabRepository)
    }
}


