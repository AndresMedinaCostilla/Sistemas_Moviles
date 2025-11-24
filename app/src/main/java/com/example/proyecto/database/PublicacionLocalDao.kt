package com.example.proyecto.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PublicacionLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(publicacion: PublicacionLocal): Long

    @Update
    suspend fun actualizar(publicacion: PublicacionLocal)

    @Delete
    suspend fun eliminar(publicacion: PublicacionLocal)

    @Query("SELECT * FROM publicaciones_locales WHERE idUsuario = :userId ORDER BY fechaCreacion DESC")
    suspend fun obtenerTodasPorUsuario(userId: Int): List<PublicacionLocal>

    @Query("SELECT * FROM publicaciones_locales WHERE idUsuario = :userId AND esBorrador = 1 ORDER BY fechaCreacion DESC")
    suspend fun obtenerBorradores(userId: Int): List<PublicacionLocal>

    @Query("SELECT * FROM publicaciones_locales WHERE idUsuario = :userId AND esBorrador = 0 AND estadoSincronizacion = 'pendiente' ORDER BY fechaCreacion DESC")
    suspend fun obtenerPendientesSincronizacion(userId: Int): List<PublicacionLocal>

    @Query("SELECT * FROM publicaciones_locales WHERE id = :id")
    suspend fun obtenerPorId(id: Int): PublicacionLocal?

    @Query("DELETE FROM publicaciones_locales WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Query("SELECT COUNT(*) FROM publicaciones_locales WHERE idUsuario = :userId AND esBorrador = 0 AND estadoSincronizacion = 'pendiente'")
    suspend fun contarPendientes(userId: Int): Int
}