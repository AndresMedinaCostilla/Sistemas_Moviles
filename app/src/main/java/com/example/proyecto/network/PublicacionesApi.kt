package com.example.proyecto.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PublicacionesApi {

    /**
     * Crear una nueva publicación con imágenes
     */
    @Multipart
    @POST("api/publicaciones")
    suspend fun crearPublicacion(
        @Part("id_usuario") idUsuario: RequestBody,
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part imagenes: List<MultipartBody.Part>
    ): Response<CrearPublicacionResponse>

    /**
     * Obtener todas las publicaciones
     */
    @GET("api/publicaciones")
    suspend fun obtenerPublicaciones(): Response<ObtenerPublicacionesResponse>

    /**
     * Obtener publicaciones de un usuario específico
     */
    @GET("api/publicaciones/usuario/{id}")
    suspend fun obtenerPublicacionesUsuario(
        @Path("id") userId: Int
    ): Response<ObtenerPublicacionesResponse>
}

// ==================== DATA CLASSES ====================

/**
 * Response al crear publicación
 */
data class CrearPublicacionResponse(
    val success: Boolean,
    val message: String,
    val publicacion: PublicacionResponse?
)

/**
 * Response al obtener publicaciones
 */
data class ObtenerPublicacionesResponse(
    val success: Boolean,
    val data: List<PublicacionResponse>
)

/**
 * Modelo de publicación del servidor
 */
data class PublicacionResponse(
    val id_publicacion: Int,
    val titulo: String,
    val descripcion: String?,
    val fecha_publicacion: String,
    val cantidad_likes: Int,
    val cantidad_comentarios: Int,
    val cantidad_favoritos: Int,
    val usuario: UsuarioPublicacion?,
    val imagenes: List<String>
)

/**
 * Datos del usuario en una publicación
 */
data class UsuarioPublicacion(
    val id_usuario: Int,
    val nombre: String,
    val apellido_paterno: String,
    val usuario: String,
    val foto_perfil: String?
)