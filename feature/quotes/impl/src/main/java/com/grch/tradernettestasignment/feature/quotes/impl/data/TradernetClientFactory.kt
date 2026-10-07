package com.grch.tradernettestasignment.feature.quotes.impl.data

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Process-wide Tradernet networking boundary.
 *
 * The single [OkHttpClient] backs both the live quotes WebSocket (via
 * [TradernetQuoteSocketDataSource]) and the [retrofit] instance reserved for the
 * later REST capability. No authentication or credential interceptor is
 * installed: every Tradernet quotes endpoint used here is unauthenticated.
 */
object TradernetClientFactory {

    const val BASE_URL: String = "https://tradernet.com/"
    const val SOCKET_URL: String = "wss://wss.tradernet.com/"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Keep the idle WebSocket alive without an application-level ping.
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit instance wired now for the later REST capability; no service is bound yet. */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
    }
}
