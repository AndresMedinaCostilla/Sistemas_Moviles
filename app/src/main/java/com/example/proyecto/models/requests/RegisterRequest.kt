package com.example.proyecto.models.requests

data class RegisterRequest(
    val nombre: String,
    val apellido_paterno: String,
    val apellido_materno: String?,
    val usuario: String,
    val correo_electronico: String,
    val contrasena: String,
    val telefono: String?
    // Por ahora omitimos foto_perfil, la agregaremos después
)