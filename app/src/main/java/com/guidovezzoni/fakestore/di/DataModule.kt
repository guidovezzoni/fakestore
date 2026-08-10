package com.guidovezzoni.fakestore.di

import com.guidovezzoni.fakestore.data.network.ApiService
import com.guidovezzoni.fakestore.data.repository.ProductRepositoryImpl
import com.guidovezzoni.fakestore.domain.repository.ProductRepository
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    fun provideProductRepository(apiService: ApiService): ProductRepository = ProductRepositoryImpl(apiService)

    @Provides
    fun provideGetProductsUseCase(repository: ProductRepository): GetProductsUseCase = GetProductsUseCase(repository)
}
