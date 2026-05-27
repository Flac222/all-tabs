package com.uade.alltabs.core.di

import com.uade.alltabs.data.remote.CoverArtArchiveApi
import com.uade.alltabs.data.remote.MusicBrainzApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("MusicBrainz")
    fun provideMusicBrainzRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("CoverArt")
    fun provideCoverArtRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://coverartarchive.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicBrainzApi(@Named("MusicBrainz") retrofit: Retrofit): MusicBrainzApi {
        return retrofit.create(MusicBrainzApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCoverArtArchiveApi(@Named("CoverArt") retrofit: Retrofit): CoverArtArchiveApi {
        return retrofit.create(CoverArtArchiveApi::class.java)
    }
}
