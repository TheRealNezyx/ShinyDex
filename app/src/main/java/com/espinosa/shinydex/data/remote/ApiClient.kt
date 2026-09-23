package com.espinosa.shinydex.data.remote

import com.espinosa.shinydex.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single shared Retrofit stack.
 *
 * Security notes (see security/SAC.md):
 *  - HTTPS only; cleartext is additionally blocked by res/xml/network_security_config.xml.
 *  - Request/response logging is compiled in for debug builds only, so a release build
 *    cannot leak traffic into logcat.
 *  - Finite timeouts, so a hostile or hung endpoint cannot pin a coroutine forever.
 */
object ApiClient {

    private const val BASE_URL = "https://pokeapi.co/api/v2/"
    private const val TIMEOUT_SECONDS = 20L

    val service: PokeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokeApiService::class.java)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }
}
