package com.grch.tradernettestassignment.feature.quotes.data.remote

import com.grch.tradernettestassignment.feature.quotes.data.parser.subscriptionFrame
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** A connection lifecycle signal or a raw text frame from the Tradernet socket. */
internal sealed interface SocketEvent {
    data class Frame(val text: String) : SocketEvent
    data object Opened : SocketEvent
    data object Closed : SocketEvent

    /** Terminal: [WebSocketRemoteDataSource.maxAttempts] consecutive attempts failed; no more retries follow. */
    data object GaveUp : SocketEvent
}

/** Owns the Tradernet WebSocket connection and its retries. */
internal class WebSocketRemoteDataSource internal constructor(
    private val client: OkHttpClient,
    private val maxAttempts: Int,
    private val reconnectDelay: Duration,
) {
    @Inject
    constructor(client: OkHttpClient) : this(client, DEFAULT_MAX_ATTEMPTS, DEFAULT_RECONNECT_DELAY)

/**
     * Emits lifecycle events and raw text frames, reconnecting while collected. [SocketEvent.Closed] follows
     * every ended attempt. An attempt fails if it never reaches onOpen; an open resets the failure count.
     * After [maxAttempts] consecutive failures, [SocketEvent.GaveUp] is emitted and the flow completes.
     */
    internal fun observeFrames(tickers: List<String>): Flow<SocketEvent> = flow {
        var failedAttempts = 0
        while (currentCoroutineContext().isActive) {
            var opened = false
            openConnection(tickers)
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    Log.d(TAG, "socket ended: ${failure.message}")
                }
                .collect { event ->
                    if (event == SocketEvent.Opened) opened = true
                    emit(event)
                }
            emit(SocketEvent.Closed)
            failedAttempts = if (opened) 0 else failedAttempts + 1
            if (failedAttempts >= maxAttempts) {
                emit(SocketEvent.GaveUp)
                return@flow
            }
            if (currentCoroutineContext().isActive) delay(reconnectDelay)
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
        const val DEFAULT_MAX_ATTEMPTS = 3
        val DEFAULT_RECONNECT_DELAY = 2.seconds
    }
}
