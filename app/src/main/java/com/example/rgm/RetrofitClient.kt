package com.pushkar.RGM

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // For Emulator use: http://10.0.2.2:3000/
    // For Physical Device via USB run: adb reverse tcp:3000 tcp:3000
    // Then you can use: http://127.0.0.1:3000/
    const val BASE_URL = "http://10.0.2.2:3000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val instance: UserApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(UserApiService::class.java)
    }
}
