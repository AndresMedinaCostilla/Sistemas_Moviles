package com.example.proyecto.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 🔥 IMPORTANTE: Cambia esta URL según tu configuración
    // - Emulador Android: "http://10.0.2.2:3000/"
    // - Dispositivo físico: "http://TU_IP_LOCAL:3000/" (ej: http://192.168.1.100:3000/)
    const val BASE_URL: String = "http://10.0.2.2:3000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val perfilApi: PerfilApi by lazy {
        retrofit.create(PerfilApi::class.java)
    }

    val publicacionesApi: PublicacionesApi by lazy {
        retrofit.create(PublicacionesApi::class.java)
    }


    val reaccionesApi: ReaccionesApi by lazy {
        retrofit.create(ReaccionesApi::class.java)
    }
}