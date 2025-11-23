package com.example.proyecto.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesAdapter
import com.example.proyecto.models.Publicacion
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.network.ReaccionRequest
import com.example.proyecto.network.FavoritoRequest
import com.example.proyecto.utils.SessionManager
import com.example.proyecto.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class BusquedaFragment : Fragment() {

    private lateinit var etBuscar: EditText
    private lateinit var btnToggleFavoritos: ImageView
    private lateinit var txtFiltroActivo: TextView
    private lateinit var recyclerResultados: RecyclerView
    private lateinit var layoutNoResultados: LinearLayout
    private lateinit var txtNoResultados: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: PublicacionesAdapter
    private lateinit var sessionManager: SessionManager

    // Nuevas vistas para sin conexión
    private lateinit var layoutSinConexion: View
    private lateinit var btnReintentar: Button
    private lateinit var searchBar: LinearLayout

    private var buscarSoloFavoritos = false
    private var todasLasPublicaciones = mutableListOf<Publicacion>()
    private var publicacionesFavoritasLocales = mutableSetOf<String>()
    private var idUsuarioActual: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_busqueda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        val userData = sessionManager.getUserData()
        idUsuarioActual = userData?.idUsuario ?: 0

        if (idUsuarioActual == 0) {
            Toast.makeText(context, "Error: No hay sesión activa", Toast.LENGTH_LONG).show()
            return
        }

        // Inicializar vistas
        etBuscar = view.findViewById(R.id.etBuscar)
        btnToggleFavoritos = view.findViewById(R.id.btnToggleFavoritos)
        txtFiltroActivo = view.findViewById(R.id.txtFiltroActivo)
        recyclerResultados = view.findViewById(R.id.recyclerResultados)
        layoutNoResultados = view.findViewById(R.id.layoutNoResultados)
        txtNoResultados = view.findViewById(R.id.txtNoResultados)
        progressBar = view.findViewById(R.id.progressBar)
        searchBar = view.findViewById(R.id.searchBar)

        // Vistas de sin conexión
        layoutSinConexion = view.findViewById(R.id.layoutSinConexion)
        btnReintentar = view.findViewById(R.id.btnReintentar)

        setupRecyclerView()
        setupBusqueda()
        setupToggleFavoritos()

        // Configurar botón de reintentar
        btnReintentar.setOnClickListener {
            verificarConexionYCargar()
        }

        // Verificar conexión antes de cargar
        verificarConexionYCargar()
    }

    // ==================== VERIFICACIÓN Y CARGA ====================

    private fun verificarConexionYCargar() {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            println("✅ Conexión disponible: ${NetworkUtils.getConnectionType(requireContext())}")
            ocultarMensajeSinConexion()
            cargarFavoritosLocales()
            cargarPublicacionesDesdeBackend()
        } else {
            println("❌ Sin conexión a internet")
            mostrarMensajeSinConexion()
        }
    }

    private fun mostrarMensajeSinConexion() {
        searchBar.visibility = View.GONE
        layoutSinConexion.visibility = View.VISIBLE
        recyclerResultados.visibility = View.GONE
        layoutNoResultados.visibility = View.GONE
        progressBar.visibility = View.GONE

        Toast.makeText(
            context,
            "Sin conexión a internet",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun ocultarMensajeSinConexion() {
        searchBar.visibility = View.VISIBLE
        layoutSinConexion.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = PublicacionesAdapter(
            publicaciones = emptyList(),
            onLikeClick = { publicacion ->
                if (!NetworkUtils.isInternetAvailable(requireContext())) {
                    Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                    return@PublicacionesAdapter
                }
                manejarLike(publicacion)
            },
            onDislikeClick = { publicacion ->
                if (!NetworkUtils.isInternetAvailable(requireContext())) {
                    Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                    return@PublicacionesAdapter
                }
                manejarDislike(publicacion)
            },
            onCommentClick = { publicacion ->
                val bundle = bundleOf(
                    "idPublicacion" to publicacion.id.toInt(),
                    "tituloPublicacion" to publicacion.titulo
                )
                findNavController().navigate(
                    R.id.action_busquedaFragment_to_commentsFragment,
                    bundle
                )
            },
            onFavoriteClick = { publicacion ->
                if (!NetworkUtils.isInternetAvailable(requireContext())) {
                    Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                    return@PublicacionesAdapter
                }
                manejarFavorito(publicacion)
            },
            onPublicacionClick = { publicacion ->
                Toast.makeText(context, "Ver: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerResultados.layoutManager = LinearLayoutManager(context)
        recyclerResultados.adapter = adapter
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarResultados(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupToggleFavoritos() {
        btnToggleFavoritos.setOnClickListener {
            buscarSoloFavoritos = !buscarSoloFavoritos
            actualizarEstadoToggle()
            actualizarResultados(etBuscar.text.toString())
        }
    }

    private fun actualizarEstadoToggle() {
        if (buscarSoloFavoritos) {
            btnToggleFavoritos.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
            txtFiltroActivo.text = "Buscando en: Solo favoritos ⭐"
            txtFiltroActivo.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
        } else {
            btnToggleFavoritos.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
            txtFiltroActivo.text = "Buscando en: Todas las publicaciones"
            txtFiltroActivo.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
        }
    }

    // ==================== CARGAR PUBLICACIONES DESDE BACKEND ====================

    private fun cargarPublicacionesDesdeBackend() {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesApi.obtenerPublicaciones()

                if (response.isSuccessful && response.body() != null) {
                    val publicacionesResponse = response.body()!!.data
                    val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")

                    todasLasPublicaciones = publicacionesResponse.map { pubResp ->
                        val imagenesCompletas = pubResp.imagenes.map { url ->
                            if (url.startsWith("http")) url else "$baseUrl$url"
                        }

                        val fotoPerfilUsuario = if (!pubResp.usuario?.foto_perfil.isNullOrEmpty()) {
                            "$baseUrl${pubResp.usuario?.foto_perfil}"
                        } else {
                            null
                        }

                        Publicacion(
                            id = pubResp.id_publicacion.toString(),
                            titulo = pubResp.titulo,
                            descripcion = pubResp.descripcion ?: "",
                            imagenesUrl = imagenesCompletas,
                            fecha = formatearFecha(pubResp.fecha_publicacion),
                            likes = pubResp.cantidad_likes,
                            dislikes = 0,
                            comentarios = pubResp.cantidad_comentarios,
                            usuarioId = pubResp.usuario?.id_usuario?.toString() ?: "0",
                            usuarioNombre = if (pubResp.usuario != null) {
                                pubResp.usuario.usuario
                            } else {
                                "Usuario"
                            },
                            usuarioFoto = fotoPerfilUsuario,
                            favoritos = pubResp.cantidad_favoritos,
                            usuarioLike = false,
                            usuarioDislike = false,
                            usuarioFavorito = false
                        )
                    }.toMutableList()

                    println("✅ ${todasLasPublicaciones.size} publicaciones cargadas desde el servidor")

                    cargarEstadoReacciones(todasLasPublicaciones)

                    mostrarCargando(false)

                    if (etBuscar.text.toString().isEmpty()) {
                        mostrarMensajeInicial()
                    } else {
                        actualizarResultados(etBuscar.text.toString())
                    }

                } else {
                    mostrarCargando(false)
                    mostrarError("Error al cargar publicaciones del servidor")
                    println("❌ Error HTTP: ${response.code()}")
                }
            } catch (e: Exception) {
                mostrarCargando(false)
                if (!NetworkUtils.isInternetAvailable(requireContext())) {
                    mostrarMensajeSinConexion()
                } else {
                    mostrarError("Error de conexión: ${e.message}")
                }
                e.printStackTrace()
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

                            if (estado.es_favorito) {
                                publicacionesFavoritasLocales.add(pub.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error al obtener estado de publicación ${pub.id}: ${e.message}")
                }
            }

            guardarFavoritosLocales()
            adapter.notifyDataSetChanged()

            println("✅ Estados de reacciones cargados")
        }
    }

    // ==================== MANEJO DE REACCIONES ====================

    private fun manejarLike(publicacion: Publicacion) {
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

                        val position = adapter.publicaciones.indexOf(publicacion)
                        if (position != -1) {
                            adapter.notifyItemChanged(position)
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

                        val position = adapter.publicaciones.indexOf(publicacion)
                        if (position != -1) {
                            adapter.notifyItemChanged(position)
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

                        if (data.es_favorito) {
                            publicacionesFavoritasLocales.add(publicacion.id)
                        } else {
                            publicacionesFavoritasLocales.remove(publicacion.id)
                        }
                        guardarFavoritosLocales()

                        if (buscarSoloFavoritos && !data.es_favorito) {
                            actualizarResultados(etBuscar.text.toString())
                        } else {
                            val position = adapter.publicaciones.indexOf(publicacion)
                            if (position != -1) {
                                adapter.notifyItemChanged(position)
                            }
                        }

                        val mensaje = if (data.es_favorito) {
                            "⭐ Agregado a favoritos"
                        } else {
                            "Eliminado de favoritos"
                        }
                        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
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

    // ==================== ACTUALIZAR RESULTADOS ====================

    private fun actualizarResultados(query: String) {
        if (todasLasPublicaciones.isEmpty()) {
            return
        }

        val publicacionesBase = if (buscarSoloFavoritos) {
            todasLasPublicaciones.filter { it.usuarioFavorito }
        } else {
            todasLasPublicaciones
        }

        val resultados = if (query.isEmpty()) {
            publicacionesBase
        } else {
            publicacionesBase.filter {
                it.titulo.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true) ||
                        it.usuarioNombre.contains(query, ignoreCase = true)
            }
        }

        if (resultados.isEmpty()) {
            recyclerResultados.visibility = View.GONE
            layoutNoResultados.visibility = View.VISIBLE

            txtNoResultados.text = when {
                buscarSoloFavoritos && !todasLasPublicaciones.any { it.usuarioFavorito } ->
                    "No tienes publicaciones en favoritos"
                buscarSoloFavoritos ->
                    "No se encontraron resultados en favoritos"
                query.isEmpty() && todasLasPublicaciones.isEmpty() ->
                    "No hay publicaciones disponibles"
                query.isEmpty() ->
                    "Escribe algo para buscar publicaciones"
                else ->
                    "No se encontraron resultados para \"$query\""
            }
        } else {
            recyclerResultados.visibility = View.VISIBLE
            layoutNoResultados.visibility = View.GONE
            adapter.updatePublicaciones(resultados)
        }
    }

    private fun mostrarMensajeInicial() {
        recyclerResultados.visibility = View.GONE
        layoutNoResultados.visibility = View.VISIBLE
        txtNoResultados.text = "Escribe algo para buscar publicaciones"
    }

    // ==================== FAVORITOS LOCALES ====================

    private fun cargarFavoritosLocales() {
        val prefs = requireContext().getSharedPreferences("favoritos_$idUsuarioActual", android.content.Context.MODE_PRIVATE)
        val favoritosString = prefs.getString("publicaciones_favoritas", "")

        if (!favoritosString.isNullOrEmpty()) {
            publicacionesFavoritasLocales = favoritosString.split(",").filter { it.isNotEmpty() }.toMutableSet()
            println("📚 ${publicacionesFavoritasLocales.size} favoritos locales cargados")
        }
    }

    private fun guardarFavoritosLocales() {
        val prefs = requireContext().getSharedPreferences("favoritos_$idUsuarioActual", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("publicaciones_favoritas", publicacionesFavoritasLocales.joinToString(",")).apply()
    }

    // ==================== UTILIDADES ====================

    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recyclerResultados.visibility = if (mostrar) View.GONE else recyclerResultados.visibility
        layoutNoResultados.visibility = if (mostrar) View.GONE else layoutNoResultados.visibility
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
        layoutNoResultados.visibility = View.VISIBLE
        recyclerResultados.visibility = View.GONE
        txtNoResultados.text = mensaje
    }

    private fun formatearFecha(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd 'de' MMM yyyy, h:mm a", Locale("es", "MX"))
            val date = inputFormat.parse(fechaISO)
            outputFormat.format(date)
        } catch (e: Exception) {
            try {
                val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd 'de' MMM yyyy, h:mm a", Locale("es", "MX"))
                val date = inputFormat2.parse(fechaISO)
                outputFormat.format(date)
            } catch (e2: Exception) {
                fechaISO
            }
        }
    }
}