package com.guidovezzoni.fakestore.di

import com.guidovezzoni.fakestore.core.network.NetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkClient(): NetworkClient = NetworkClient()

    @Provides
    @Singleton
    fun provideRetrofit(networkClient: NetworkClient): Retrofit = networkClient.retrofit
}
