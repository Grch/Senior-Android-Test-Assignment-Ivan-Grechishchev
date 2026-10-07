package com.grch.tradernettestasignment.feature.quotes.impl.data

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Process-wide Tradernet networking boundary.
 *
 * The single [OkHttpClient] backs both the live quotes WebSocket (via
 * [TradernetQuoteSocketDataSource]) and the [retrofit] instance used for the
 * top-securities REST capability. No authentication or credential interceptor is
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

    /** Retrofit instance bound to the top-securities REST service. */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
    }

    /**
     * Retrofit service for `GET https://tradernet.com/api/` with a serialized `q`
     * query parameter. Uses Retrofit's built-in `ResponseBody` converter, so no
     * JSON converter dependency is needed.
     */
    val topSecuritiesService: TopSecuritiesService by lazy {
        retrofit.create(TopSecuritiesService::class.java)
    }
}
