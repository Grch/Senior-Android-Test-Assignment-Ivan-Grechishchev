package com.grch.tradernettestasignment.feature.quotes.data.remote

import com.grch.tradernettestasignment.feature.quotes.data.parser.subscriptionFrame
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

/** A connection lifecycle signal or a raw text frame from the Tradernet socket. */
internal sealed interface SocketEvent {
    data class Frame(val text: String) : SocketEvent
    data object Opened : SocketEvent
    data object Closed : SocketEvent
}

/** Owns the raw, unauthenticated Tradernet WebSocket connection and its retries. */
class WebSocketRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
) {
    /**
     * Emits connection lifecycle events and unmodified text frames, reconnecting
     * after a disconnect while collected. [SocketEvent.Closed] is emitted after
     * every ended attempt, whether or not it ever reached [SocketEvent.Opened] —
     * callers use it to detect a failed/no-network first attempt.
     */
    internal fun observeFrames(tickers: List<String>): Flow<SocketEvent> = flow {
        while (currentCoroutineContext().isActive) {
            openConnection(tickers)
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    Log.d(TAG, "socket ended: ${failure.message}")
                }
                .collect { event -> emit(event) }
            emit(SocketEvent.Closed)
            if (currentCoroutineContext().isActive) delay(RECONNECT_DELAY_MS)
        }
    }

    private fun openConnection(tickers: List<String>): Flow<SocketEvent> = callbackFlow {
        val request = Request.Builder()
            .url(TradernetUrls.SOCKET_URL)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "socket open")
                trySend(SocketEvent.Opened)
                webSocket.send(subscriptionFrame(tickers))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(SocketEvent.Frame(text))
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
