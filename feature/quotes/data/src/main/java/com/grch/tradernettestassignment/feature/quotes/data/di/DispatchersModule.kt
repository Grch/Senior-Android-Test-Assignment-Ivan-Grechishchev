package com.grch.tradernettestassignment.feature.quotes.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** Dispatcher for CPU-bound work such as JSON parsing. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {
    @Provides
    @DefaultDispatcher
    @Suppress("InjectDispatcher") // This is the single place the real dispatcher is supplied.
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
