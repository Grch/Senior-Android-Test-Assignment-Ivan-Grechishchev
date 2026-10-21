package com.grch.tradernettestassignment.feature.quotes.data.remote

import com.grch.tradernettestassignment.feature.quotes.data.model.QuoteFrameDto
import com.grch.tradernettestassignment.feature.quotes.data.model.QuotePayloadDto
import com.grch.tradernettestassignment.feature.quotes.data.parser.QuoteFrameJsonAdapter
import com.grch.tradernettestassignment.feature.quotes.data.parser.QuoteMessageParser
import com.grch.tradernettestassignment.feature.quotes.data.repository.TradernetQuoteRepository
import com.grch.tradernettestassignment.feature.quotes.domain.model.Quote
import com.grch.tradernettestassignment.feature.quotes.domain.model.QuoteStreamEvent
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs the real [WebSocketRemoteDataSource] against a MockWebServer (an interceptor redirects the fixed URL).
 * Uses [Dispatchers.Unconfined] and a short [RECONNECT_DELAY] because the tests rely on real time.
 */
class WebSocketRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: WebSocketRemoteDataSource
    private lateinit var repository: TradernetQuoteRepository

    @Before
    @Suppress("InjectDispatcher") // Real-time test: see class KDoc.
    fun setUp() {
        server = MockWebServer()
        server.start()
        val target = server.url("/")
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().url(target).build())
            }
            .build()
        dataSource = WebSocketRemoteDataSource(client, maxAttempts = 3, reconnectDelay = RECONNECT_DELAY)
        val moshi = Moshi.Builder().build()
        val frameAdapter: JsonAdapter<QuoteFrameDto> =
            QuoteFrameJsonAdapter(moshi.adapter(QuotePayloadDto::class.java))
        repository = TradernetQuoteRepository(dataSource, QuoteMessageParser(frameAdapter), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        server.close()
    }

    /** Server-side listener recording what the client sent and how the socket ended. */
    private class ServerSide(private val onOpenSend: List<String> = emptyList()) : WebSocketListener() {
        val received = Channel<String>(Channel.UNLIMITED)
        val ended = Channel<String>(Channel.UNLIMITED)

        override fun onOpen(webSocket: WebSocket, response: Response) {
            onOpenSend.forEach { webSocket.send(it) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            received.trySend(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            ended.trySend("closing:$code")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            ended.trySend("closed:$code")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            ended.trySend("failure")
        }
    }

    private fun upgrade(listener: WebSocketListener) {
        server.enqueue(MockResponse.Builder().webSocketUpgrade(listener).build())
    }

    private suspend fun awaitEvents(events: List<SocketEvent>, count: Int) {
        while (events.size < count) delay(20)
    }

    @Test
    fun connectsAndSendsSubscriptionFrameThenEmitsServerFrames() = runBlocking {
        val serverSide = ServerSide(listOf("""["q",{"c":"SBER","ltp":1.5}]""", "hello"))
        upgrade(serverSide)

        val events = java.util.Collections.synchronizedList(mutableListOf<SocketEvent>())
        coroutineScope {
            val job = launch { dataSource.observeFrames(listOf("SBER", "GAZP")).collect { events.add(it) } }
            withTimeout(TIMEOUT_MS) {
                assertEquals("""["quotes",["SBER","GAZP"]]""", serverSide.received.receive())
                awaitEvents(events, 3)
            }
            job.cancelAndJoin()
        }

        assertEquals(
            listOf(
                SocketEvent.Opened,
                SocketEvent.Frame("""["q",{"c":"SBER","ltp":1.5}]"""),
                SocketEvent.Frame("hello"),
            ),
            events.toList(),
        )
    }

    @Test
    fun serverCloseEmitsClosedThenReconnectsAndResubscribes() = runBlocking {
        val first = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.close(1000, "bye")
            }
        }
        val second = ServerSide()
        upgrade(first)
        upgrade(second)

        val events = java.util.Collections.synchronizedList(mutableListOf<SocketEvent>())
        coroutineScope {
            val job = launch { dataSource.observeFrames(listOf("SBER")).collect { events.add(it) } }
            withTimeout(TIMEOUT_MS) {
                assertEquals("""["quotes",["SBER"]]""", second.received.receive())
                awaitEvents(events, 3)
            }
            job.cancelAndJoin()
        }

        assertEquals(listOf(SocketEvent.Opened, SocketEvent.Closed, SocketEvent.Opened), events.toList())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun failedAttemptEmitsClosedWithoutOpenedThenRetries() = runBlocking {
        server.enqueue(MockResponse.Builder().code(500).build())
        upgrade(ServerSide())

        val events = withTimeout(TIMEOUT_MS) {
            dataSource.observeFrames(listOf("SBER")).take(2).toList()
        }

        assertEquals(listOf(SocketEvent.Closed, SocketEvent.Opened), events)
    }

    @Test
    fun cancellingCollectorClosesTheSocket() = runBlocking {
        val serverSide = ServerSide()
        upgrade(serverSide)
        val opened = Channel<Unit>(Channel.CONFLATED)

        val job = launch {
            dataSource.observeFrames(listOf("SBER")).collect { if (it == SocketEvent.Opened) opened.trySend(Unit) }
        }
        withTimeout(TIMEOUT_MS) {
            opened.receive()
            serverSide.received.receive()
        }
        job.cancel()
        withTimeout(TIMEOUT_MS) {
            job.join()
            // client.cancel() drops the connection; the server observes a close or a failure.
            serverSide.ended.receive()
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun repositoryMapsFramesAndLifecycleEvents() = runBlocking {
        val serverSide = ServerSide(
            listOf(
                """["q",{"c":"SBER","ltr":"MCX","ltp":10.5,"chg":0.5,"pcp":5.0,"pp":10.0,"min_step":0.01}]""",
                """["userStats",{"c":"SBER"}]""",
                "garbage",
                """["q",{"c":"SBER","ltp":11.0}]""",
            ),
        )
        upgrade(serverSide)

        val events = withTimeout(TIMEOUT_MS) {
            repository.observeQuotes(listOf("SBER")).take(3).toList()
        }

        assertEquals(
            listOf(
                QuoteStreamEvent.Connected,
                QuoteStreamEvent.Update(Quote("SBER", "MCX", null, 10.5, 0.5, 5.0, 10.0, 0.01)),
                QuoteStreamEvent.Update(Quote("SBER", lastPrice = 11.0)),
            ),
            events,
        )
    }

    @Test
    fun repositoryEmitsDisconnectedWhenConnectionCannotBeEstablished() = runBlocking {
        server.enqueue(MockResponse.Builder().code(500).build())

        val events = withTimeout(TIMEOUT_MS) {
            repository.observeQuotes(listOf("SBER")).take(1).toList()
        }

        assertEquals(listOf<QuoteStreamEvent>(QuoteStreamEvent.Disconnected), events)
    }

    @Test
    fun threeConsecutiveFailedAttemptsEmitGaveUpOnceAndComplete() = runBlocking {
        repeat(3) { server.enqueue(MockResponse.Builder().code(500).build()) }

        val events = withTimeout(TIMEOUT_MS) {
            dataSource.observeFrames(listOf("SBER")).toList()
        }

        assertEquals(
            listOf(SocketEvent.Closed, SocketEvent.Closed, SocketEvent.Closed, SocketEvent.GaveUp),
            events,
        )
        assertEquals(3, server.requestCount)
    }

    @Test
    fun aSuccessfulOpenResetsTheFailedAttemptCount() = runBlocking {
        val dropsImmediately = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.close(1000, "bye")
            }
        }
        repeat(2) { server.enqueue(MockResponse.Builder().code(500).build()) }
        upgrade(dropsImmediately)
        repeat(3) { server.enqueue(MockResponse.Builder().code(500).build()) }

        val events = withTimeout(TIMEOUT_MS) {
            dataSource.observeFrames(listOf("SBER")).toList()
        }

        assertEquals(
            listOf(
                SocketEvent.Closed,
                SocketEvent.Closed,
                SocketEvent.Opened,
                SocketEvent.Closed,
                SocketEvent.Closed,
                SocketEvent.Closed,
                SocketEvent.Closed,
                SocketEvent.GaveUp,
            ),
            events,
        )
        assertEquals(6, server.requestCount)
    }

    @Test
    fun repositoryMapsGiveUpToConnectionLost() = runBlocking {
        repeat(3) { server.enqueue(MockResponse.Builder().code(500).build()) }

        val events = withTimeout(TIMEOUT_MS) {
            repository.observeQuotes(listOf("SBER")).toList()
        }

        assertEquals(
            listOf(
                QuoteStreamEvent.Disconnected,
                QuoteStreamEvent.Disconnected,
                QuoteStreamEvent.Disconnected,
                QuoteStreamEvent.ConnectionLost,
            ),
            events,
        )
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        val RECONNECT_DELAY = 50.milliseconds
    }
}
