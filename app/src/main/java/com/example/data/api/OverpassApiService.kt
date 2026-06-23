package com.example.data.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val id: Long,
    val type: String,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>?
)

data class NominatimResponse(
    val lat: String,
    val lon: String,
    val display_name: String
)

interface OverpassApiService {
    @GET("api/interpreter")
    suspend fun getPharmacies(
        @Query("data") query: String
    ): OverpassResponse
}

interface NominatimApiService {
    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1
    ): List<NominatimResponse>
}

object NominatimClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "HealthWalletApp/1.0 (healthwallet@example.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: NominatimApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApiService::class.java)
    }
}


object OverpassClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "HealthWalletApp/1.0 (healthwallet@example.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: OverpassApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OverpassApiService::class.java)
    }
}
