package com.example.proyecto.models

data class Publicacion(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val imagenesUrl: List<String>, // Cambiado de imagenUrl a imagenesUrl (lista)
    val fecha: String,
    var likes: Int,
    var dislikes: Int,
    val comentarios: Int,
    val usuarioId: String,
    val usuarioNombre: String,
    val usuarioFoto: String? = null,

    var favoritos: Int = 0,
    var usuarioLike: Boolean = false,
    var usuarioDislike: Boolean = false,
    var usuarioFavorito: Boolean = false
)