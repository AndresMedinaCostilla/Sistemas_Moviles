package com.example.proyecto.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesPerfilAdapter
import com.example.proyecto.models.Publicacion
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.network.ReaccionRequest
import com.example.proyecto.network.FavoritoRequest
import com.example.proyecto.utils.SessionManager
import com.example.proyecto.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.proyecto.repository.PublicacionRepository
import com.example.proyecto.database.PublicacionLocal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PerfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    private lateinit var publicacionRepository: PublicacionRepository
    // RecyclerView principal
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerAdapter: HeaderAdapter

    private lateinit var publicacionesAdapter: PublicacionesPerfilAdapter
    private lateinit var progressBar: ProgressBar

    // Navegación inferior
    private lateinit var btnNavHome: ImageView
    private lateinit var btnNavAdd: ImageView
    private lateinit var btnNavSearch: ImageView

    // Estado de la pestaña activa
    private enum class TabActivo { PUBLICACIONES, FAVORITOS, BORRADORES }
    private var tabActual = TabActivo.PUBLICACIONES

    private var todasPublicaciones = mutableListOf<Publicacion>()
    private var publicacionesFavoritas = mutableListOf<Publicacion>()
    private var publicacionesBorradores = mutableListOf<Publicacion>()
    private var idUsuarioActual: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        publicacionRepository = PublicacionRepository(requireContext())

        // Obtener ID del usuario actual
        val userData = sessionManager.getUserData()
        idUsuarioActual = userData?.idUsuario ?: 0

        if (idUsuarioActual == 0) {
            Toast.makeText(context, "Error: No hay sesión activa", Toast.LENGTH_LONG).show()
            return
        }

        inicializarVistas(view)
        configurarRecyclerView()
        configurarNavegacion()
        cargarPublicaciones()
    }

    private fun inicializarVistas(view: View) {
        recyclerView = view.findViewById(R.id.recyclerPublicaciones)
        btnNavHome = view.findViewById(R.id.btnNavHome)
        btnNavAdd = view.findViewById(R.id.btnNavAdd)
        btnNavSearch = view.findViewById(R.id.btnNavSearch)
        progressBar = view.findViewById(R.id.progressBar) ?: ProgressBar(requireContext())
    }

    private fun configurarRecyclerView() {
        // Crear header adapter
        headerAdapter = HeaderAdapter(
            sessionManager = sessionManager,
            onEditarClick = {
                findNavController().navigate(R.id.action_perfilFragment_to_editarPerfilFragment)
            },
            onCerrarSesionClick = {
                sessionManager.logout()
                Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
            },
            onTabClick = { tab ->
                when (tab) {
                    0 -> seleccionarTab(TabActivo.PUBLICACIONES)
                    1 -> seleccionarTab(TabActivo.FAVORITOS)
                    2 -> seleccionarTab(TabActivo.BORRADORES)
                }
            }
        )

        // Crear publicaciones adapter con opciones de editar/eliminar
        publicacionesAdapter = PublicacionesPerfilAdapter(
            publicaciones = emptyList(),
            idUsuarioActual = idUsuarioActual.toString(),
            onLikeClick = { publicacion ->
                manejarLike(publicacion)
            },
            onDislikeClick = { publicacion ->
                manejarDislike(publicacion)
            },
            onCommentClick = { publicacion ->
                val bundle = Bundle().apply {
                    putInt("idPublicacion", publicacion.id.toInt())
                    putString("tituloPublicacion", publicacion.titulo)
                }
                findNavController().navigate(
                    R.id.action_perfilFragment_to_commentsFragment,
                    bundle
                )
            },
            onFavoriteClick = { publicacion ->
                manejarFavorito(publicacion)
            },
            onPublicacionClick = { publicacion ->
                Toast.makeText(context, "Ver: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            },
            onEditarClick = { publicacion ->
                val esBorrador = publicacion.id.startsWith("draft_")

                val bundle = Bundle().apply {
                    if (esBorrador) {
                        putString("borrador_id", publicacion.id)
                        putString("borrador_titulo", publicacion.titulo)
                        putString("borrador_contenido", publicacion.descripcion)
                    } else {
                        putString("publicacion_id", publicacion.id)
                        putString("publicacion_titulo", publicacion.titulo)
                        putString("publicacion_contenido", publicacion.descripcion)
                    }
                }

                if (esBorrador) {
                    findNavController().navigate(
                        R.id.action_perfilFragment_to_editarBorradorFragment,
                        bundle
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_perfilFragment_to_editarPublicacionFragment,
                        bundle
                    )
                }
            },
            onEliminarClick = { publicacion ->
                eliminarPublicacion(publicacion)
            }
        )

        // Combinar ambos adapters
        val concatAdapter = ConcatAdapter(headerAdapter, publicacionesAdapter)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = concatAdapter
    }

    private fun configurarNavegacion() {
        btnNavHome.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_homeFragment)
        }

        btnNavAdd.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_agregarPublicacionFragment)
        }

        btnNavSearch.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_busquedaFragment)
        }
    }

    private fun seleccionarTab(tab: TabActivo) {
        tabActual = tab

        // Actualizar UI del header
        headerAdapter.actualizarTab(tab.ordinal)

        // Mostrar publicaciones según el tab
        when (tab) {
            TabActivo.PUBLICACIONES -> {
                headerAdapter.actualizarTitulo("Mis Publicaciones")
                // Solo publicaciones del usuario actual
                val misPublicaciones = todasPublicaciones.filter {
                    it.usuarioId == idUsuarioActual.toString()
                }
                mostrarPublicaciones(misPublicaciones)
            }
            TabActivo.FAVORITOS -> {
                headerAdapter.actualizarTitulo("Mis Favoritos")
                // Cargar favoritos desde el servidor
                cargarFavoritosDesdeServidor()
            }
            TabActivo.BORRADORES -> {
                headerAdapter.actualizarTitulo("Mis Borradores")
                // Cargar borradores desde la base de datos local
                cargarBorradoresLocales()
            }
        }
    }

    private fun cargarBorradoresLocales() {
        lifecycleScope.launch {
            try {
                val borradoresLocales = publicacionRepository.obtenerBorradores(idUsuarioActual)

                // Convertir PublicacionLocal a Publicacion
                publicacionesBorradores = borradoresLocales.map { local ->
                    convertirPublicacionLocalAPublicacion(local)
                }.toMutableList()

                mostrarPublicaciones(publicacionesBorradores)

                if (publicacionesBorradores.isEmpty()) {
                    Toast.makeText(context, "No tienes borradores guardados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al cargar borradores", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertirPublicacionLocalAPublicacion(local: PublicacionLocal): Publicacion {
        // Obtener las URIs de las imágenes desde el JSON
        val gson = Gson()
        val tipoLista = object : TypeToken<List<String>>() {}.type
        val rutasImagenes: List<String> = gson.fromJson(local.imagenesUris, tipoLista)

        // Convertir rutas de archivo a URIs que pueda usar Glide
        val urisImagenes = rutasImagenes.map { ruta ->
            "file://$ruta" // Convertir a URI que Glide pueda cargar
        }

        // Obtener datos del usuario actual para la foto de perfil
        val userData = sessionManager.getUserData()
        var fotoPerfilUrl: String? = null

        if (!userData?.fotoPerfil.isNullOrEmpty()) {
            val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
            fotoPerfilUrl = "$baseUrl${userData?.fotoPerfil}"
        }

        return Publicacion(
            id = "draft_${local.id}", // Prefijo para identificar borradores
            titulo = local.titulo,
            descripcion = local.contenido,
            imagenesUrl = urisImagenes,
            fecha = formatearFechaLocal(local.fechaCreacion),
            likes = 0,
            dislikes = 0,
            comentarios = 0,
            favoritos = 0,
            usuarioId = idUsuarioActual.toString(),
            usuarioNombre = userData?.usuario ?: "Usuario",
            usuarioFoto = fotoPerfilUrl, // Ahora carga la foto de perfil real
            usuarioLike = false,
            usuarioDislike = false,
            usuarioFavorito = false
        )
    }

    // Función para formatear fecha local
    private fun formatearFechaLocal(timestamp: Long): String {
        return try {
            val outputFormat = SimpleDateFormat("dd 'de' MMMM 'del' yyyy 'a las' HH:mm", Locale("es", "MX"))
            "Creado el ${outputFormat.format(timestamp)}"
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }

    private fun cargarFavoritosDesdeServidor() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesApi.obtenerFavoritosUsuario(idUsuarioActual)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")

                        publicacionesFavoritas = responseBody.data.map { pub ->
                            val imagenesCompletas = pub.imagenes.map { url ->
                                "$baseUrl$url"
                            }

                            val fotoPerfilUsuario = if (!pub.usuario?.foto_perfil.isNullOrEmpty()) {
                                "$baseUrl${pub.usuario?.foto_perfil}"
                            } else {
                                null
                            }

                            Publicacion(
                                id = pub.id_publicacion.toString(),
                                titulo = pub.titulo,
                                descripcion = pub.descripcion ?: "",
                                imagenesUrl = imagenesCompletas,
                                fecha = formatearFecha(pub.fecha_publicacion),
                                likes = pub.cantidad_likes,
                                dislikes = 0,
                                comentarios = pub.cantidad_comentarios,
                                favoritos = pub.cantidad_favoritos,
                                usuarioId = pub.usuario?.id_usuario.toString() ?: "",
                                usuarioNombre = pub.usuario?.usuario ?: "Usuario",
                                usuarioFoto = fotoPerfilUsuario,
                                usuarioLike = false,
                                usuarioDislike = false,
                                usuarioFavorito = true
                            )
                        }.toMutableList()

                        // Cargar estado de reacciones para los favoritos
                        cargarEstadoReacciones(publicacionesFavoritas)

                        mostrarCargando(false)
                        mostrarPublicaciones(publicacionesFavoritas)
                    } else {
                        mostrarCargando(false)
                        publicacionesFavoritas.clear()
                        mostrarPublicaciones(publicacionesFavoritas)
                        Toast.makeText(context, "No tienes favoritos aún", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    mostrarCargando(false)
                    Toast.makeText(context, "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mostrarCargando(false)
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarPublicaciones() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesApi.obtenerPublicaciones()

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")

                        todasPublicaciones = responseBody.data.map { pub ->
                            val imagenesCompletas = pub.imagenes.map { url ->
                                "$baseUrl$url"
                            }

                            val fotoPerfilUsuario = if (!pub.usuario?.foto_perfil.isNullOrEmpty()) {
                                "$baseUrl${pub.usuario?.foto_perfil}"
                            } else {
                                null
                            }

                            Publicacion(
                                id = pub.id_publicacion.toString(),
                                titulo = pub.titulo,
                                descripcion = pub.descripcion ?: "",
                                imagenesUrl = imagenesCompletas,
                                fecha = formatearFecha(pub.fecha_publicacion),
                                likes = pub.cantidad_likes,
                                dislikes = 0,
                                comentarios = pub.cantidad_comentarios,
                                favoritos = pub.cantidad_favoritos,
                                usuarioId = pub.usuario?.id_usuario.toString() ?: "",
                                usuarioNombre = pub.usuario?.usuario ?: "Usuario",
                                usuarioFoto = fotoPerfilUsuario,
                                usuarioLike = false,
                                usuarioDislike = false,
                                usuarioFavorito = false
                            )
                        }.toMutableList()

                        // Cargar estado de reacciones
                        cargarEstadoReacciones(todasPublicaciones)

                        // Mostrar publicaciones del usuario
                        seleccionarTab(TabActivo.PUBLICACIONES)

                        mostrarCargando(false)
                    } else {
                        mostrarCargando(false)
                        Toast.makeText(context, "No hay publicaciones", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    mostrarCargando(false)
                    Toast.makeText(context, "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mostrarCargando(false)
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarEstadoReacciones(publicaciones: List<Publicacion>) {
        lifecycleScope.launch {
            publicaciones.forEach { pub ->
                try {
                    val response = RetrofitClient.reaccionesApi.obtenerEstadoReacciones(
                        pub.id.toInt(),
                        idUsuarioActual
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val estado = response.body()!!.data

                        if (estado != null) {
                            pub.usuarioLike = estado.reaccion_usuario == "like"
                            pub.usuarioDislike = estado.reaccion_usuario == "dislike"
                            pub.usuarioFavorito = estado.es_favorito
                            pub.dislikes = estado.dislikes
                        }
                    }
                } catch (e: Exception) {
                    // Error silencioso para no interrumpir la carga
                }
            }

            publicacionesAdapter.notifyDataSetChanged()
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {
        if (mostrar) {
            // Mostrar spinner y ocultar RecyclerView temporalmente
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        } else {
            // Ocultar spinner y mostrar RecyclerView
            progressBar.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    private fun mostrarPublicaciones(publicaciones: List<Publicacion>) {
        publicacionesAdapter.updatePublicaciones(publicaciones)
    }

    // ==================== MANEJO DE REACCIONES ====================

    private fun manejarLike(publicacion: Publicacion) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val idPublicacion = publicacion.id.toInt()
                val request = ReaccionRequest(
                    id_usuario = idUsuarioActual,
                    tipo_reaccion = "like"
                )

                val response = RetrofitClient.reaccionesApi.reaccionarPublicacion(
                    idPublicacion,
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data

                    if (data != null) {
                        publicacion.likes = data.likes
                        publicacion.dislikes = data.dislikes
                        publicacion.usuarioLike = data.reaccion_usuario == "like"
                        publicacion.usuarioDislike = data.reaccion_usuario == "dislike"

                        val position = publicacionesAdapter.publicaciones.indexOf(publicacion)
                        if (position != -1) {
                            publicacionesAdapter.notifyItemChanged(position)
                        }

                        val mensaje = when {
                            publicacion.usuarioLike -> "👍 Like agregado"
                            else -> "Like eliminado"
                        }
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

    private fun manejarDislike(publicacion: Publicacion) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val idPublicacion = publicacion.id.toInt()
                val request = ReaccionRequest(
                    id_usuario = idUsuarioActual,
                    tipo_reaccion = "dislike"
                )

                val response = RetrofitClient.reaccionesApi.reaccionarPublicacion(
                    idPublicacion,
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data

                    if (data != null) {
                        publicacion.likes = data.likes
                        publicacion.dislikes = data.dislikes
                        publicacion.usuarioLike = data.reaccion_usuario == "like"
                        publicacion.usuarioDislike = data.reaccion_usuario == "dislike"

                        val position = publicacionesAdapter.publicaciones.indexOf(publicacion)
                        if (position != -1) {
                            publicacionesAdapter.notifyItemChanged(position)
                        }

                        val mensaje = when {
                            publicacion.usuarioDislike -> "👎 Dislike agregado"
                            else -> "Dislike eliminado"
                        }
                        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error al procesar dislike", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun manejarFavorito(publicacion: Publicacion) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val idPublicacion = publicacion.id.toInt()
                val request = FavoritoRequest(id_usuario = idUsuarioActual)

                val response = RetrofitClient.reaccionesApi.toggleFavorito(
                    idPublicacion,
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data

                    if (data != null) {
                        publicacion.favoritos = data.favoritos
                        publicacion.usuarioFavorito = data.es_favorito

                        // Si estamos en la pestaña de favoritos
                        if (tabActual == TabActivo.FAVORITOS) {
                            if (!data.es_favorito) {
                                // Si se quitó el favorito, eliminar de la lista y recargar
                                publicacionesFavoritas.removeAll { it.id == publicacion.id }
                                mostrarPublicaciones(publicacionesFavoritas)

                                Toast.makeText(
                                    context,
                                    "Eliminado de favoritos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Actualizar el item
                                val position = publicacionesAdapter.publicaciones.indexOf(publicacion)
                                if (position != -1) {
                                    publicacionesAdapter.notifyItemChanged(position)
                                }

                                Toast.makeText(
                                    context,
                                    "⭐ Agregado a favoritos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Si estamos en otra pestaña, solo actualizar el contador
                            val position = publicacionesAdapter.publicaciones.indexOf(publicacion)
                            if (position != -1) {
                                publicacionesAdapter.notifyItemChanged(position)
                            }

                            val mensaje = if (data.es_favorito) {
                                "⭐ Agregado a favoritos"
                            } else {
                                "Eliminado de favoritos"
                            }
                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Error al procesar favorito", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun eliminarPublicacion(publicacion: Publicacion) {
        val esBorrador = publicacion.id.startsWith("draft_")

        // Mostrar diálogo de confirmación
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar ${if (esBorrador) "borrador" else "publicación"}")
            .setMessage("¿Estás seguro de que deseas eliminar \"${publicacion.titulo}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                if (esBorrador) {
                    eliminarBorrador(publicacion)
                } else {
                    eliminarPublicacionServidor(publicacion)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarBorrador(publicacion: Publicacion) {
        val idBorrador = publicacion.id.removePrefix("draft_").toIntOrNull()

        if (idBorrador != null) {
            lifecycleScope.launch {
                try {
                    publicacionRepository.eliminarBorrador(idBorrador)
                    publicacionesBorradores.removeAll { it.id == publicacion.id }

                    Toast.makeText(
                        context,
                        "✅ Borrador \"${publicacion.titulo}\" eliminado",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Actualizar vista
                    seleccionarTab(tabActual)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "❌ Error al eliminar borrador",
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(context, "Error: ID de borrador inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminarPublicacionServidor(publicacion: Publicacion) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                println("🗑️ Eliminando publicación ID: ${publicacion.id}")

                val response = RetrofitClient.publicacionesApi.eliminarPublicacion(
                    publicacion.id.toInt()
                )

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        // Eliminar de todas las listas
                        todasPublicaciones.removeAll { it.id == publicacion.id }
                        publicacionesFavoritas.removeAll { it.id == publicacion.id }

                        Toast.makeText(
                            context,
                            "✅ \"${publicacion.titulo}\" eliminada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Actualizar vista según el tab actual
                        seleccionarTab(tabActual)
                    } else {
                        Toast.makeText(
                            context,
                            "❌ Error: ${responseBody.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("❌ Error al eliminar: $errorBody")
                    Toast.makeText(
                        context,
                        "❌ Error al eliminar la publicación (Código: ${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }


    private fun formatearFecha(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd 'de' MMMM 'del' yyyy 'a las' HH:mm", Locale("es", "MX"))
            val date = inputFormat.parse(fechaISO)
            "Creado el ${outputFormat.format(date)}"
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }

    // ==================== HEADER ADAPTER ====================

    inner class HeaderAdapter(
        private val sessionManager: SessionManager,
        private val onEditarClick: () -> Unit,
        private val onCerrarSesionClick: () -> Unit,
        private val onTabClick: (Int) -> Unit
    ) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

        private var tabSeleccionado = 0
        private var tituloSeccion = "Mis Publicaciones"

        inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgFotoPerfil: ImageView = view.findViewById(R.id.imgFotoPerfil)
            val txtNombre: TextView = view.findViewById(R.id.txtNombre)
            val txtUsuario: TextView = view.findViewById(R.id.txtUsuario)
            val txtMiembroDesde: TextView = view.findViewById(R.id.txtMiembroDesde)
            val btnEditarPerfil: ImageButton = view.findViewById(R.id.btnEditarPerfil)
            val btnCerrarSesion: Button = view.findViewById(R.id.btnCerrarSesion)

            val btnPublicaciones: LinearLayout = view.findViewById(R.id.btnPublicaciones)
            val btnFavoritos: LinearLayout = view.findViewById(R.id.btnFavoritos)
            val btnBorradores: LinearLayout = view.findViewById(R.id.btnBorradores)
            val iconPublicaciones: ImageView = view.findViewById(R.id.iconPublicaciones)
            val textPublicaciones: TextView = view.findViewById(R.id.textPublicaciones)
            val iconFavoritos: ImageView = view.findViewById(R.id.iconFavoritos)
            val textFavoritos: TextView = view.findViewById(R.id.textFavoritos)
            val iconBorradores: ImageView = view.findViewById(R.id.iconBorradores)
            val textBorradores: TextView = view.findViewById(R.id.textBorradores)

            val tvTituloSeccion: TextView = view.findViewById(R.id.tvTituloSeccion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.perfil_header, parent, false)
            return HeaderViewHolder(view)
        }

        override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
            val userData = sessionManager.getUserData()

            if (userData != null) {
                val nombreCompleto = "${userData.nombre} ${userData.apellidoPaterno ?: ""}"
                holder.txtNombre.text = nombreCompleto.trim()
                holder.txtUsuario.text = "@${userData.usuario}"

                val fechaRegistro = userData.fechaRegistro ?: "2024"
                val anio = try {
                    fechaRegistro.substring(0, 4)
                } catch (e: Exception) {
                    "2024"
                }
                holder.txtMiembroDesde.text = "Miembro desde $anio"

                val fotoPerfil = userData.fotoPerfil
                if (!fotoPerfil.isNullOrEmpty()) {
                    val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                    val fullImageUrl = "$baseUrl$fotoPerfil"

                    Glide.with(holder.itemView.context)
                        .load(fullImageUrl)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(holder.imgFotoPerfil)
                } else {
                    holder.imgFotoPerfil.setImageResource(R.drawable.user)
                }
            }

            holder.btnEditarPerfil.setOnClickListener { onEditarClick() }
            holder.btnCerrarSesion.setOnClickListener { onCerrarSesionClick() }

            holder.btnPublicaciones.setOnClickListener { onTabClick(0) }
            holder.btnFavoritos.setOnClickListener { onTabClick(1) }
            holder.btnBorradores.setOnClickListener { onTabClick(2) }

            actualizarEstiloTabs(holder)
            holder.tvTituloSeccion.text = tituloSeccion
        }

        private fun actualizarEstiloTabs(holder: HeaderViewHolder) {
            resetearTab(holder.iconPublicaciones, holder.textPublicaciones)
            resetearTab(holder.iconFavoritos, holder.textFavoritos)
            resetearTab(holder.iconBorradores, holder.textBorradores)

            when (tabSeleccionado) {
                0 -> activarTab(holder.iconPublicaciones, holder.textPublicaciones)
                1 -> activarTab(holder.iconFavoritos, holder.textFavoritos)
                2 -> activarTab(holder.iconBorradores, holder.textBorradores)
            }
        }

        private fun resetearTab(icon: ImageView, text: TextView) {
            icon.setColorFilter(ContextCompat.getColor(icon.context, android.R.color.darker_gray))
            text.setTextColor(ContextCompat.getColor(text.context, android.R.color.darker_gray))
            text.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        private fun activarTab(icon: ImageView, text: TextView) {
            icon.setColorFilter(ContextCompat.getColor(icon.context, R.color.cyan))
            text.setTextColor(ContextCompat.getColor(text.context, R.color.cyan))
            text.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        fun actualizarTab(tab: Int) {
            tabSeleccionado = tab
            notifyItemChanged(0)
        }

        fun actualizarTitulo(titulo: String) {
            tituloSeccion = titulo
            notifyItemChanged(0)
        }

        override fun getItemCount(): Int = 1
    }
}