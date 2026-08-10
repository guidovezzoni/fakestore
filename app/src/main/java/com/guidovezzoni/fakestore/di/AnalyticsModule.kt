package com.guidovezzoni.fakestore.di

import com.guidovezzoni.fakestore.BuildConfig
import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.core.analytics.DebugAnalyticsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsClient(): AnalyticsClient = provideAnalyticsClient(BuildConfig.DEBUG)
}

internal fun provideAnalyticsClient(isDebug: Boolean): AnalyticsClient {
    val analyticsClient = AnalyticsClient()
    if (isDebug) {
        analyticsClient.register(DebugAnalyticsProvider())
    }
    return analyticsClient
}
