package com.example.proyecto.models

data class Comentario(
    val id: String,
    val usuarioId: String,
    val usuarioNombre: String,
    val usuarioFoto: String?,
    val texto: String,
    val fecha: String,
    var likes: Int,
    val publicacionId: String,
    var respuestas: MutableList<Respuesta> = mutableListOf(),
    var tieneLike: Boolean = false
)

data class Respuesta(
    val id: String,
    val usuarioId: String,
    val usuarioNombre: String,
    val usuarioFoto: String?,
    val texto: String,
    val fecha: String,
    val comentarioId: String,
    var likes: Int = 0,
    var tieneLike: Boolean = false
)