package com.example.proyecto.network

import retrofit2.Response
import retrofit2.http.*

interface ComentariosApi {

    /**
     * Crear un nuevo comentario o respuesta
     */
    @POST("api/comentarios")
    suspend fun crearComentario(
        @Body request: CrearComentarioRequest
    ): Response<CrearComentarioResponse>

    /**
     * Obtener todos los comentarios de una publicación
     */
    @GET("api/comentarios/publicacion/{idPublicacion}")
    suspend fun obtenerComentarios(
        @Path("idPublicacion") idPublicacion: Int
    ): Response<ObtenerComentariosResponse>

    /**
     * Dar o quitar like a un comentario
     */
    @POST("api/comentarios/like/{idComentario}")
    suspend fun likeComentario(
        @Path("idComentario") idComentario: Int,
        @Body request: LikeComentarioRequest
    ): Response<LikeComentarioResponse>

    /**
     * Obtener estado de likes del usuario en una publicación
     */
    @GET("api/comentarios/likes/usuario/{idUsuario}/publicacion/{idPublicacion}")
    suspend fun obtenerLikesUsuario(
        @Path("idUsuario") idUsuario: Int,
        @Path("idPublicacion") idPublicacion: Int
    ): Response<LikesUsuarioResponse>

    /**
     * Eliminar un comentario
     */
    @HTTP(method = "DELETE", path = "api/comentarios/{idComentario}", hasBody = true)
    suspend fun eliminarComentario(
        @Path("idComentario") idComentario: Int,
        @Body request: EliminarComentarioRequest
    ): Response<EliminarComentarioResponse>
}

// ==================== DATA CLASSES ====================

/**
 * Request para crear comentario o respuesta
 */
data class CrearComentarioRequest(
    val id_publicacion: Int,
    val id_usuario: Int,
    val descripcion: String,
    val id_comentario_padre: Int? = null  // null = comentario principal, con valor = respuesta
)

/**
 * Response al crear comentario
 */
data class CrearComentarioResponse(
    val success: Boolean,
    val message: String,
    val data: ComentarioResponse?
)

/**
 * Response al obtener comentarios
 */
data class ObtenerComentariosResponse(
    val success: Boolean,
    val data: List<ComentarioResponse>
)

/**
 * Modelo de comentario del servidor
 */
data class ComentarioResponse(
    val id_comentario: Int,
    val id_publicacion: Int,
    val id_comentario_padre: Int?,
    val descripcion: String,
    val cantidad_likes: Int,
    val cantidad_dislikes: Int,
    val fecha_comentario: String,
    val id_usuario: Int,
    val nombre: String,
    val apellido_paterno: String,
    val usuario: String,
    val foto_perfil: String?,
    val respuestas: List<ComentarioResponse>? = null  // Solo para comentarios principales
)

/**
 * Request para like en comentario
 */
data class LikeComentarioRequest(
    val id_usuario: Int
)

/**
 * Response de like en comentario
 */
data class LikeComentarioResponse(
    val success: Boolean,
    val message: String,
    val data: LikeComentarioData?
)

data class LikeComentarioData(
    val likes: Int,
    val tiene_like: Boolean
)

/**
 * Response de likes del usuario
 */
data class LikesUsuarioResponse(
    val success: Boolean,
    val data: Map<String, Boolean>  // Map de id_comentario -> tiene_like
)

/**
 * Request para eliminar comentario
 */
data class EliminarComentarioRequest(
    val id_usuario: Int
)

/**
 * Response al eliminar comentario
 */
data class EliminarComentarioResponse(
    val success: Boolean,
    val message: String
)