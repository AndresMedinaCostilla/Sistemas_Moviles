package com.example.proyecto.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.proyecto.models.responses.UsuarioData
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_DATA = "user_data"
    }

    /**
     * Guarda la sesión completa del usuario
     */
    fun saveUserSession(userData: UsuarioData) {
        val userJson = gson.toJson(userData)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_DATA, userJson)
            apply()
        }
    }

    /**
     * Verifica si hay una sesión activa
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Obtiene todos los datos del usuario
     */
    fun getUserData(): UsuarioData? {
        val userJson = prefs.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, UsuarioData::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    fun getNombreCompleto(): String {
        val userData = getUserData()
        return if (userData != null) {
            buildString {
                append(userData.nombre)
                if (!userData.apellidoPaterno.isNullOrEmpty()) {
                    append(" ${userData.apellidoPaterno}")
                }
                if (!userData.apellidoMaterno.isNullOrEmpty()) {
                    append(" ${userData.apellidoMaterno}")
                }
            }.trim()
        } else {
            "Usuario"
        }
    }

    /**
     * Obtiene solo el nombre del usuario
     */
    fun getNombre(): String {
        return getUserData()?.nombre ?: "Usuario"
    }

    fun getUser(): String
    {
        return getUserData()?.usuario ?: "No se encontro el nombre de usuario"
    }

    /**
     * Obtiene la URL de la foto de perfil
     */
    fun getFotoPerfil(): String? {
        return getUserData()?.fotoPerfil
    }

    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(): Int {
        return getUserData()?.idUsuario ?: -1
    }

    /**
     * Cierra la sesión del usuario
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}