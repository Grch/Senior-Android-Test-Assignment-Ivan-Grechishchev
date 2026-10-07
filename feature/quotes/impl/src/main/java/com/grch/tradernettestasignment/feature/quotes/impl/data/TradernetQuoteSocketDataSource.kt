package com.grch.tradernettestasignment.feature.quotes.impl.data

import android.util.Log
import com.grch.tradernettestasignment.feature.quotes.impl.domain.Quote
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * No-auth stream of Tradernet quote updates.
 *
 * The socket is opened lazily when the returned flow is collected and closed
 * when the collection is cancelled, so the ViewModel fully owns when the
 * connection is alive.
 */
interface QuoteSocketDataSource {
    /**
     * Subscribes to the supplied active ticker set and emits one [Quote] per
     * incoming `q` payload. The [tickers] value is placed into the same verified
     * subscription frame; the wire format is unchanged by this capability. Each
     * emission may be partial; the consumer merges by ticker.
     */
    fun observeQuotes(tickers: List<String>): Flow<Quote>
}

/**
 * OkHttp-backed [QuoteSocketDataSource] for `wss://wss.tradernet.com/`.
 *
 * Wire format is documented at
 * `https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en` and
 * verified against a live capture: the subscription is one text frame holding
 * `["quotes", ["<ticker>", ...]]`, and updates arrive as
 * `["q", { ...named fields... }, "wstm=..."]`. The active ticker set is supplied
 * per collection; only its contents change, not the framing.
 */
class TradernetQuoteSocketDataSource(
    private val client: OkHttpClient = TradernetClientFactory.okHttpClient,
) : QuoteSocketDataSource {

    override fun observeQuotes(tickers: List<String>): Flow<Quote> = callbackFlow {
        val request = Request.Builder()
            .url(TradernetClientFactory.SOCKET_URL)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "socket open")
                webSocket.send(subscriptionFrame(tickers))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseQuoteMessage(text)?.let { quote -> trySend(quote) }
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
    }
}
