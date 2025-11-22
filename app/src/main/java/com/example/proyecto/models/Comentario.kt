package com.example.proyecto.models

data class Comentario(
    val id: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val usuarioFoto: String = "",
    val texto: String = "",
    val fecha: String = "",
    val likes: Int = 0,
    val publicacionId: String = "",
    val respuestas: MutableList<Respuesta> = mutableListOf()
)

data class Respuesta(
    val id: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val usuarioFoto: String = "",
    val texto: String = "",
    val fecha: String = "",
    val comentarioId: String = ""
)