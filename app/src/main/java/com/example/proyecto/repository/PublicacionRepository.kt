package com.example.proyecto.repository

import android.content.Context
import android.net.Uri
import com.example.proyecto.database.AppDatabase
import com.example.proyecto.database.PublicacionLocal
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class PublicacionRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).publicacionLocalDao()

    /**
     * Guarda una publicación localmente
     */
    suspend fun guardarPublicacionLocal(
        idUsuario: Int,
        titulo: String,
        contenido: String,
        imagenesUris: List<Uri>,
        esBorrador: Boolean
    ): Long {
        return withContext(Dispatchers.IO) {
            // Copiar imágenes a almacenamiento persistente
            val urisPersistentes = copiarImagenesAPersistente(imagenesUris)

            val publicacion = PublicacionLocal(
                idUsuario = idUsuario,
                titulo = titulo,
                contenido = contenido,
                imagenesUris = Gson().toJson(urisPersistentes),
                esBorrador = esBorrador,
                estadoSincronizacion = if (esBorrador) "borrador" else "pendiente"
            )

            dao.insertar(publicacion)
        }
    }

    /**
     * Copia imágenes de URIs temporales a almacenamiento interno
     */
    private fun copiarImagenesAPersistente(uris: List<Uri>): List<String> {
        val rutasPersistentes = mutableListOf<String>()
        val directorioImagenes = File(context.filesDir, "publicaciones_locales")

        if (!directorioImagenes.exists()) {
            directorioImagenes.mkdirs()
        }

        uris.forEach { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val nombreArchivo = "img_${System.currentTimeMillis()}_${rutasPersistentes.size}.jpg"
                val archivo = File(directorioImagenes, nombreArchivo)

                val outputStream = FileOutputStream(archivo)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                rutasPersistentes.add(archivo.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return rutasPersistentes
    }

    /**
     * Obtiene todos los borradores del usuario
     */
    suspend fun obtenerBorradores(userId: Int): List<PublicacionLocal> {
        return withContext(Dispatchers.IO) {
            dao.obtenerBorradores(userId)
        }
    }

    /**
     * Obtiene publicaciones pendientes de sincronización
     */
    suspend fun obtenerPendientesSincronizacion(userId: Int): List<PublicacionLocal> {
        return withContext(Dispatchers.IO) {
            dao.obtenerPendientesSincronizacion(userId)
        }
    }

    /**
     * Cuenta publicaciones pendientes
     */
    suspend fun contarPendientes(userId: Int): Int {
        return withContext(Dispatchers.IO) {
            dao.contarPendientes(userId)
        }
    }

    /**
     * Intenta sincronizar publicaciones pendientes con el servidor
     */
    suspend fun sincronizarPublicacionesPendientes(userId: Int): SincronizacionResult {
        if (!NetworkUtils.isInternetAvailable(context)) {
            return SincronizacionResult(0, 0, "Sin conexión a internet")
        }

        val pendientes = obtenerPendientesSincronizacion(userId)
        var exitosas = 0
        var fallidas = 0

        pendientes.forEach { publicacion ->
            try {
                val resultado = subirPublicacionAlServidor(publicacion)

                if (resultado) {
                    // Marcar como sincronizada y eliminar
                    dao.eliminarPorId(publicacion.id)
                    exitosas++
                } else {
                    // Marcar como error
                    val actualizada = publicacion.copy(estadoSincronizacion = "error")
                    dao.actualizar(actualizada)
                    fallidas++
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fallidas++
            }
        }

        val mensaje = when {
            exitosas > 0 && fallidas == 0 -> "$exitosas publicaciones sincronizadas"
            exitosas > 0 && fallidas > 0 -> "$exitosas sincronizadas, $fallidas fallaron"
            fallidas > 0 -> "Error al sincronizar $fallidas publicaciones"
            else -> "No hay publicaciones pendientes"
        }

        return SincronizacionResult(exitosas, fallidas, mensaje)
    }

    /**
     * Sube una publicación local al servidor
     */
    private suspend fun subirPublicacionAlServidor(publicacion: PublicacionLocal): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Preparar datos
                val idUsuarioBody = publicacion.idUsuario.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val tituloBody = publicacion.titulo
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val descripcionBody = publicacion.contenido
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                // Preparar imágenes desde rutas persistentes
                val imagenesParts = mutableListOf<MultipartBody.Part>()
                val rutasImagenes: List<String> = Gson().fromJson(
                    publicacion.imagenesUris,
                    object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                )

                rutasImagenes.forEach { ruta ->
                    val file = File(ruta)
                    if (file.exists()) {
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData(
                            "imagenes",
                            file.name,
                            requestFile
                        )
                        imagenesParts.add(part)
                    }
                }

                // Enviar al servidor
                val response = RetrofitClient.publicacionesApi.crearPublicacion(
                    idUsuario = idUsuarioBody,
                    titulo = tituloBody,
                    descripcion = descripcionBody,
                    imagenes = imagenesParts
                )

                response.isSuccessful && response.body()?.success == true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Elimina un borrador
     */
    suspend fun eliminarBorrador(id: Int) {
        withContext(Dispatchers.IO) {
            dao.eliminarPorId(id)
        }
    }

    /**
     * Publica un borrador (lo convierte en pendiente de sincronización)
     */
    suspend fun publicarBorrador(id: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val publicacion = dao.obtenerPorId(id)
                if (publicacion != null && publicacion.esBorrador) {
                    val actualizada = publicacion.copy(
                        esBorrador = false,
                        estadoSincronizacion = "pendiente"
                    )
                    dao.actualizar(actualizada)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}

data class SincronizacionResult(
    val exitosas: Int,
    val fallidas: Int,
    val mensaje: String
)