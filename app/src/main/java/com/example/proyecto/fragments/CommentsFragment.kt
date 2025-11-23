package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.ComentariosAdapter
import com.example.proyecto.models.Comentario
import com.example.proyecto.models.Respuesta
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.network.CrearComentarioRequest
import com.example.proyecto.network.LikeComentarioRequest
import com.example.proyecto.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class CommentsFragment : Fragment() {

    private lateinit var recyclerComentarios: RecyclerView
    private lateinit var adapter: ComentariosAdapter
    private lateinit var etComment: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var sessionManager: SessionManager

    private var idUsuarioActual: Int = 0
    private var idPublicacionActual: Int = 0
    private var tituloPublicacion: String = "Comentarios"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val userData = sessionManager.getUserData()
        idUsuarioActual = userData?.idUsuario ?: 0

        if (idUsuarioActual == 0) {
            Toast.makeText(context, "Error: No hay sesión activa", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        // ✅ Recibir idPublicacion desde los argumentos
        idPublicacionActual = arguments?.getInt("idPublicacion", 0) ?: 0
        tituloPublicacion = arguments?.getString("tituloPublicacion", "Comentarios") ?: "Comentarios"

        println("🔍 DEBUG - Arguments recibidos:")
        println("   idPublicacion: $idPublicacionActual")
        println("   tituloPublicacion: $tituloPublicacion")
        println("   Arguments bundle: ${arguments?.keySet()?.joinToString()}")

        if (idPublicacionActual == 0) {
            Toast.makeText(context, "Error: Publicación no válida (ID: $idPublicacionActual)", Toast.LENGTH_LONG).show()
            println("❌ ERROR: idPublicacion es 0")
            findNavController().navigateUp()
            return
        }

        println("📖 Abriendo comentarios de publicación ID: $idPublicacionActual")

        recyclerComentarios = view.findViewById(R.id.recyclerComentarios)
        etComment = view.findViewById(R.id.etComment)
        btnSend = view.findViewById(R.id.btnSend)
        btnBack = view.findViewById(R.id.btnBack)



        setupRecyclerView()
        setupButtons()
        cargarComentarios()
    }

    private fun setupRecyclerView() {
        adapter = ComentariosAdapter(
            comentarios = emptyList(),
            onLikeClick = { comentario ->
                manejarLikeComentario(comentario)
            },
            onSendReply = { comentario, textoRespuesta ->
                enviarRespuesta(comentario, textoRespuesta)
            }
        )

        recyclerComentarios.layoutManager = LinearLayoutManager(context)
        recyclerComentarios.adapter = adapter
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnSend.setOnClickListener {
            val texto = etComment.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarComentario(texto)
            } else {
                Toast.makeText(context, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== CARGAR COMENTARIOS ====================

    private fun cargarComentarios() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.comentariosApi.obtenerComentarios(idPublicacionActual)

                if (response.isSuccessful && response.body() != null) {
                    val comentariosResponse = response.body()!!.data
                    val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")

                    // Convertir a modelo local
                    val comentarios = comentariosResponse.map { comentarioResp ->
                        Comentario(
                            id = comentarioResp.id_comentario.toString(),
                            usuarioId = comentarioResp.id_usuario.toString(),
                            usuarioNombre = comentarioResp.usuario,
                            usuarioFoto = if (!comentarioResp.foto_perfil.isNullOrEmpty())
                                "$baseUrl${comentarioResp.foto_perfil}" else null,
                            texto = comentarioResp.descripcion,
                            fecha = formatearFecha(comentarioResp.fecha_comentario),
                            likes = comentarioResp.cantidad_likes,
                            publicacionId = comentarioResp.id_publicacion.toString(),
                            respuestas = comentarioResp.respuestas?.map { respResp ->
                                Respuesta(
                                    id = respResp.id_comentario.toString(),
                                    usuarioId = respResp.id_usuario.toString(),
                                    usuarioNombre = respResp.usuario,
                                    usuarioFoto = if (!respResp.foto_perfil.isNullOrEmpty())
                                        "$baseUrl${respResp.foto_perfil}" else null,
                                    texto = respResp.descripcion,
                                    fecha = formatearFecha(respResp.fecha_comentario),
                                    comentarioId = respResp.id_comentario_padre.toString(),
                                    likes = respResp.cantidad_likes
                                )
                            }?.toMutableList() ?: mutableListOf(),
                            tieneLike = false
                        )
                    }

                    adapter.updateComentarios(comentarios)

                    // Cargar estado de likes
                    cargarLikesUsuario(comentarios)

                    println("✅ ${comentarios.size} comentarios cargados")
                } else {
                    Toast.makeText(context, "Error al cargar comentarios", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun cargarLikesUsuario(comentarios: List<Comentario>) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.comentariosApi.obtenerLikesUsuario(
                    idUsuarioActual,
                    idPublicacionActual
                )

                if (response.isSuccessful && response.body() != null) {
                    val likesMap = response.body()!!.data

                    // Actualizar estado de likes en comentarios
                    comentarios.forEach { comentario ->
                        comentario.tieneLike = likesMap[comentario.id] == true
                    }

                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                println("❌ Error al cargar likes: ${e.message}")
            }
        }
    }

    // ==================== ENVIAR COMENTARIO ====================

    private fun enviarComentario(texto: String) {
        lifecycleScope.launch {
            try {
                val request = CrearComentarioRequest(
                    id_publicacion = idPublicacionActual,
                    id_usuario = idUsuarioActual,
                    descripcion = texto,
                    id_comentario_padre = null
                )

                val response = RetrofitClient.comentariosApi.crearComentario(request)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(context, "Comentario enviado", Toast.LENGTH_SHORT).show()
                    etComment.text.clear()

                    // Recargar comentarios
                    cargarComentarios()
                } else {
                    Toast.makeText(context, "Error al enviar comentario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // ==================== ENVIAR RESPUESTA ====================

    private fun enviarRespuesta(comentario: Comentario, textoRespuesta: String) {
        lifecycleScope.launch {
            try {
                val request = CrearComentarioRequest(
                    id_publicacion = idPublicacionActual,
                    id_usuario = idUsuarioActual,
                    descripcion = textoRespuesta,
                    id_comentario_padre = comentario.id.toInt()
                )

                val response = RetrofitClient.comentariosApi.crearComentario(request)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(context, "Respuesta enviada", Toast.LENGTH_SHORT).show()

                    // Recargar comentarios
                    cargarComentarios()
                } else {
                    Toast.makeText(context, "Error al enviar respuesta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // ==================== MANEJAR LIKE ====================

    private fun manejarLikeComentario(comentario: Comentario) {
        lifecycleScope.launch {
            try {
                val request = LikeComentarioRequest(id_usuario = idUsuarioActual)

                val response = RetrofitClient.comentariosApi.likeComentario(
                    comentario.id.toInt(),
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data

                    if (data != null) {
                        comentario.likes = data.likes
                        comentario.tieneLike = data.tiene_like

                        adapter.notifyDataSetChanged()

                        val mensaje = if (data.tiene_like) "👍 Like" else "Like eliminado"
                        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error al procesar like", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // ==================== UTILIDADES ====================

    private fun formatearFecha(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd 'de' MMM yyyy, h:mm a", Locale("es", "MX"))
            val date = inputFormat.parse(fechaISO)
            outputFormat.format(date)
        } catch (e: Exception) {
            fechaISO
        }
    }
}