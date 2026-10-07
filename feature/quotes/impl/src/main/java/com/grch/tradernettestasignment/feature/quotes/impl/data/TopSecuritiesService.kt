package com.grch.tradernettestasignment.feature.quotes.impl.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit binding for the verified Tradernet top-securities request.
 *
 * Verified live contract: `GET https://tradernet.com/api/?q=<serialized request>`
 * — that is the [TradernetClientFactory] base URL `https://tradernet.com/` plus
 * the relative path `api/`. The documentation page
 * `https://tradernet.com/tradernet-api/quotes-get-top-securities` labels the
 * request HTTPS POST, but its executable browser example and a live GET both use
 * GET with the `q` query parameter, so the GET-plus-`q` contract is used here and
 * the documentation page URL is NOT the request URL.
 *
 * The request object is serialized into `q` by
 * [topSecuritiesRequestQuery]. [ResponseBody] is a Retrofit built-in response
 * type, so no JSON converter dependency is required; the body is parsed with
 * `org.json` in [parseTopSecuritiesResponse], consistent with the existing
 * [QuoteMessageParser]. No authentication is attached.
 */
interface TopSecuritiesService {
    @GET("api/")
    suspend fun getTopSecurities(@Query("q") q: String): Response<ResponseBody>
}
