package com.example.proyecto.network

import retrofit2.Response
import retrofit2.http.*

interface ReaccionesApi {

    /**
     * Dar o cambiar like/dislike en una publicación
     * @param idPublicacion ID de la publicación
     * @param request Contiene id_usuario y tipo_reaccion ("like" o "dislike")
     */
    @POST("api/reacciones/publicacion/{id}")
    suspend fun reaccionarPublicacion(
        @Path("id") idPublicacion: Int,
        @Body request: ReaccionRequest
    ): Response<ReaccionResponse>

    /**
     * Agregar o quitar favorito
     * @param idPublicacion ID de la publicación
     * @param request Contiene id_usuario
     */
    @POST("api/reacciones/favorito/{id}")
    suspend fun toggleFavorito(
        @Path("id") idPublicacion: Int,
        @Body request: FavoritoRequest
    ): Response<FavoritoResponse>

    /**
     * Obtener el estado de reacciones de un usuario en una publicación
     * @param idPublicacion ID de la publicación
     * @param idUsuario ID del usuario
     */
    @GET("api/reacciones/estado/{idPublicacion}/{idUsuario}")
    suspend fun obtenerEstadoReacciones(
        @Path("idPublicacion") idPublicacion: Int,
        @Path("idUsuario") idUsuario: Int
    ): Response<EstadoReaccionesResponse>
}

// ==================== DATA CLASSES ====================

/**
 * Request para dar like/dislike
 */
data class ReaccionRequest(
    val id_usuario: Int,
    val tipo_reaccion: String // "like" o "dislike"
)

/**
 * Response de reacción
 */
data class ReaccionResponse(
    val success: Boolean,
    val message: String,
    val data: ReaccionData?
)

data class ReaccionData(
    val likes: Int,
    val dislikes: Int,
    val reaccion_usuario: String? // "like", "dislike" o null
)

/**
 * Request para favorito
 */
data class FavoritoRequest(
    val id_usuario: Int
)

/**
 * Response de favorito
 */
data class FavoritoResponse(
    val success: Boolean,
    val message: String,
    val data: FavoritoData?
)

data class FavoritoData(
    val favoritos: Int,
    val es_favorito: Boolean
)

/**
 * Response del estado completo de reacciones
 */
data class EstadoReaccionesResponse(
    val success: Boolean,
    val data: EstadoReaccionesData?
)

data class EstadoReaccionesData(
    val reaccion_usuario: String?, // "like", "dislike" o null
    val es_favorito: Boolean,
    val likes: Int,
    val dislikes: Int,
    val favoritos: Int
)