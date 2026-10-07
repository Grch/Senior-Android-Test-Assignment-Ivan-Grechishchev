package com.grch.tradernettestasignment.feature.quotes.impl.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

/** Owns the raw, unauthenticated Tradernet WebSocket connection and its retries. */
class WebSocketRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
) {
    /** Emits unmodified text frames and reconnects after a disconnect while collected. */
    fun observeFrames(tickers: List<String>): Flow<String> = flow {
        while (currentCoroutineContext().isActive) {
            openConnection(tickers)
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    Log.d(TAG, "socket ended: ${failure.message}")
                }
                .collect { frame -> emit(frame) }
            if (currentCoroutineContext().isActive) delay(RECONNECT_DELAY_MS)
        }
    }

    private fun openConnection(tickers: List<String>): Flow<String> = callbackFlow {
        val request = Request.Builder()
            .url(TradernetUrls.SOCKET_URL)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "socket open")
                webSocket.send(subscriptionFrame(tickers))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.d(TAG, "socket failure: ${t.message}")
                close(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "socket closed: $code")
                close()
            }
        }

        val webSocket = client.newWebSocket(request, listener)
        awaitClose {
            Log.d(TAG, "socket cancelled")
            webSocket.cancel()
        }
    }

    private companion object {
        const val TAG = "TradernetQuotes"
        const val NORMAL_CLOSURE = 1000
        const val RECONNECT_DELAY_MS = 2_000L
    }
}
