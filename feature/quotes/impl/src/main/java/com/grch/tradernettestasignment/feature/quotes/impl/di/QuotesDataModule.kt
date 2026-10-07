package com.grch.tradernettestasignment.feature.quotes.impl.di

import com.grch.tradernettestasignment.feature.quotes.api.domain.QuoteRepository
import com.grch.tradernettestasignment.feature.quotes.api.domain.TopSecuritiesDataSource
import com.grch.tradernettestasignment.feature.quotes.impl.data.QuoteFrameDto
import com.grch.tradernettestasignment.feature.quotes.impl.data.QuoteFrameJsonAdapter
import com.grch.tradernettestasignment.feature.quotes.impl.data.QuotePayloadDto
import com.grch.tradernettestasignment.feature.quotes.impl.data.TopSecuritiesResponseDto
import com.grch.tradernettestasignment.feature.quotes.impl.data.TopSecuritiesService
import com.grch.tradernettestasignment.feature.quotes.impl.data.TradernetQuoteRepository
import com.grch.tradernettestasignment.feature.quotes.impl.data.TradernetTopSecuritiesDataSource
import com.grch.tradernettestasignment.feature.quotes.impl.data.TradernetUrls
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
abstract class QuotesDataModule {
    @Binds
    @Singleton
    abstract fun bindQuoteRepository(repository: TradernetQuoteRepository): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindTopSecuritiesDataSource(
        dataSource: TradernetTopSecuritiesDataSource,
    ): TopSecuritiesDataSource

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
            .baseUrl(TradernetUrls.BASE_URL)
            .client(client)
            .build()

        @Provides
        @Singleton
        fun provideTopSecuritiesService(retrofit: Retrofit): TopSecuritiesService =
            retrofit.create(TopSecuritiesService::class.java)

        @Provides
        @Singleton
        fun provideMoshi(): Moshi = Moshi.Builder().build()

        @Provides
        @Singleton
        fun provideQuotePayloadAdapter(moshi: Moshi): JsonAdapter<QuotePayloadDto> =
            moshi.adapter(QuotePayloadDto::class.java)

        @Provides
        @Singleton
        fun provideQuoteFrameAdapter(
            payloadAdapter: JsonAdapter<QuotePayloadDto>,
        ): JsonAdapter<QuoteFrameDto> = QuoteFrameJsonAdapter(payloadAdapter)

        @Provides
        @Singleton
        fun provideTopSecuritiesResponseAdapter(
            moshi: Moshi,
        ): JsonAdapter<TopSecuritiesResponseDto> = moshi.adapter(TopSecuritiesResponseDto::class.java)
    }
}
