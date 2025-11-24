package com.example.proyecto.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "publicaciones_locales")
data class PublicacionLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUsuario: Int,
    val titulo: String,
    val contenido: String,
    val imagenesUris: String, // Lista de URIs en formato JSON
    val fechaCreacion: Long = System.currentTimeMillis(),
    val esBorrador: Boolean = false, // true = borrador, false = pendiente de subir
    val estadoSincronizacion: String = "pendiente" // "pendiente", "sincronizado", "error"
)

// Converters para Room
class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return Gson().toJson(list)
    }
}