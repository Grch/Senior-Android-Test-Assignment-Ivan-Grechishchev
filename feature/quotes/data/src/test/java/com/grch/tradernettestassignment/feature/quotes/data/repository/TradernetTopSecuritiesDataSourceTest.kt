// Tests run under runBlocking with no virtual time, so parsing uses Dispatchers.Unconfined directly.
@file:Suppress("InjectDispatcher")

package com.grch.tradernettestassignment.feature.quotes.data.repository

import com.grch.tradernettestassignment.feature.quotes.data.model.TopSecuritiesResponseDto
import com.grch.tradernettestassignment.feature.quotes.data.parser.TopSecuritiesResponseParser
import com.grch.tradernettestassignment.feature.quotes.data.parser.topSecuritiesRequestQuery
import com.grch.tradernettestassignment.feature.quotes.data.remote.TopSecuritiesService
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecuritiesError
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecuritiesResult
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecurityRef
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class TradernetTopSecuritiesDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: TradernetTopSecuritiesDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .build()
            .create(TopSecuritiesService::class.java)
        dataSource = TradernetTopSecuritiesDataSource(
            service,
            TopSecuritiesResponseParser(Moshi.Builder().build().adapter(TopSecuritiesResponseDto::class.java)),
            Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int = 200, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun successReturnsSecuritiesAndSendsExpectedRequest() = runBlocking {
        respond(body = """{"tickers":["SBER","GAZP"],"details":{"SBER":{"name":"Sberbank"}}}""")

        val result = dataSource.fetchTopSecurities()

        assertEquals(
            TopSecuritiesResult.Success(listOf(TopSecurityRef("SBER", "Sberbank"), TopSecurityRef("GAZP"))),
            result,
        )
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/", request.url.encodedPath)
        assertEquals(topSecuritiesRequestQuery(), request.url.queryParameter("q"))
    }

    @Test
    fun httpErrorMapsToHttp() = runBlocking {
        respond(code = 503, body = "unavailable")

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.HTTP), dataSource.fetchTopSecurities())
    }

    @Test
    fun ioErrorMapsToTransport() = runBlocking {
        server.close()

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.TRANSPORT), dataSource.fetchTopSecurities())
    }

    @Test
    fun malformedBodyMapsToParse() = runBlocking {
        respond(body = "<html>not json</html>")

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.PARSE), dataSource.fetchTopSecurities())
    }

    @Test
    fun missingTickersMapsToParse() = runBlocking {
        respond(body = """{"details":{}}""")

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.PARSE), dataSource.fetchTopSecurities())
    }

    @Test
    fun blankBodyMapsToParse() = runBlocking {
        respond(body = "")

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.PARSE), dataSource.fetchTopSecurities())
    }

    @Test
    fun declaredErrorMapsToApi() = runBlocking {
        respond(body = """{"code":5,"errMsg":"denied"}""")

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.API), dataSource.fetchTopSecurities())
    }

    @Test
    fun emptyTickersMapsToEmpty() = runBlocking {
        respond(body = """{"tickers":[]}""")

        assertEquals(TopSecuritiesResult.Failure(TopSecuritiesError.EMPTY), dataSource.fetchTopSecurities())
    }

    @Test
    fun cancellationIsRethrown() {
        val service = object : TopSecuritiesService {
            override suspend fun getTopSecurities(q: String): Response<ResponseBody> =
                throw CancellationException("cancelled")
        }
        val source = TradernetTopSecuritiesDataSource(
            service,
            TopSecuritiesResponseParser(Moshi.Builder().build().adapter(TopSecuritiesResponseDto::class.java)),
            Dispatchers.Unconfined,
        )

        assertThrows(CancellationException::class.java) { runBlocking { source.fetchTopSecurities() } }
    }
}
