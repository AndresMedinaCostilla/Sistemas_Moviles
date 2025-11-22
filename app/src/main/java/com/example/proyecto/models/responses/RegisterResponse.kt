package com.example.proyecto.models.responses

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: RegisterData?
)

data class RegisterData(
    @SerializedName("id_usuario")
    val idUsuario: Int,
    val nombre: String,
    @SerializedName("apellido_paterno")
    val apellidoPaterno: String,
    @SerializedName("apellido_materno")
    val apellidoMaterno: String?,
    val usuario: String,
    @SerializedName("correo_electronico")
    val correoElectronico: String,
    @SerializedName("foto_perfil")
    val fotoPerfil: String?,
    val telefono: String?
)