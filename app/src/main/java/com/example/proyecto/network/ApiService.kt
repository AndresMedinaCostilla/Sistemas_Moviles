package com.example.proyecto.network

import com.example.proyecto.models.requests.LoginRequest
import com.example.proyecto.models.responses.LoginResponse
import com.example.proyecto.models.responses.RegisterResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Multipart
    @POST("api/registro")
    suspend fun registro(
        @Part("nombre") nombre: RequestBody,
        @Part("apellido_paterno") apellidoPaterno: RequestBody,
        @Part("apellido_materno") apellidoMaterno: RequestBody?,
        @Part("usuario") usuario: RequestBody,
        @Part("correo_electronico") correoElectronico: RequestBody,
        @Part("contrasena") contrasena: RequestBody,
        @Part("telefono") telefono: RequestBody?,
        @Part foto_perfil: MultipartBody.Part?
    ): Response<RegisterResponse>
}