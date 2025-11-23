package com.example.proyecto.network

import com.example.proyecto.models.responses.UsuarioData
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface PerfilApi {

    /**
     * Actualizar datos del perfil del usuario
     */
    @PUT("api/usuarios/{id}")
    suspend fun actualizarPerfil(
        @Path("id") userId: Int,
        @Body perfilData: ActualizarPerfilRequest
    ): Response<ActualizarPerfilResponse>

    /**
     * Actualizar foto de perfil
     */
    @Multipart
    @PUT("api/usuarios/{id}/foto")
    suspend fun actualizarFotoPerfil(
        @Path("id") userId: Int,
        @Part foto: MultipartBody.Part
    ): Response<ActualizarFotoResponse>
}

// Request para actualizar perfil
data class ActualizarPerfilRequest(
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val telefono: String? = null,
    val contrasena: String? = null // Solo si se quiere cambiar
)

// Response de actualización de perfil (ajustado al backend)
data class ActualizarPerfilResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioData
)

// Response de actualización de foto (ajustado al backend)
data class ActualizarFotoResponse(
    val success: Boolean,
    val message: String,
    val fotoPerfil: String
)