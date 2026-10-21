package com.grch.tradernettestassignment.feature.quotes.data.remote

import com.grch.tradernettestassignment.feature.quotes.data.parser.topSecuritiesRequestQuery
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** `GET https://tradernet.com/api/?q=<serialized request>`; the body is parsed by [parseTopSecuritiesResponse]. */
internal interface TopSecuritiesService {
    @GET("api/")
    suspend fun getTopSecurities(@Query("q") q: String): Response<ResponseBody>
}
