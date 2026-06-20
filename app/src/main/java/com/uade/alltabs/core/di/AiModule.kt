package com.uade.alltabs.core.di

import com.google.ai.client.generativeai.GenerativeModel
import com.uade.alltabs.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }
}
