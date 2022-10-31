package com.cookiedinner.ytcompanion.utilities

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object YoutubeRequests {

    private val retrofit: Retrofit
    private val service: RetrofitRequests

    init {
        val url = "https://www.youtube.com"

        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()
        service = retrofit.create(RetrofitRequests::class.java)
    }

    suspend fun getVideoMetadata(videoID: String) = service.getVideoMetadata(videoID)

    fun exceptionFromMessage(error: String?) : Throwable {
        var errorMessage = "Unknown error"
        if (error != null) {
            when (error) {
                "Bad Request" ->
                    errorMessage = "Video doesn't exist"
            }
            if (error.startsWith("Unable to resolve host")) {
                errorMessage = "Unable to connect"
            }
        }
        return Throwable(errorMessage)
    }
}

interface RetrofitRequests {
    @GET("/oembed")
    suspend fun getVideoMetadata(@Query("url") videoID: String): Response<YoutubeVideoMetadata>
}

