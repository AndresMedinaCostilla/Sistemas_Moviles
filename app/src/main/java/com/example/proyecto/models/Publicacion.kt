package com.example.proyecto.models

data class Publicacion (
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val imagenUrl: String = "",
    val fecha: String = "",
    val likes: Int = 0,
    val comentarios: Int = 0,
    val usuarioId: String = "",
    val usuarioNombre: String = ""
)