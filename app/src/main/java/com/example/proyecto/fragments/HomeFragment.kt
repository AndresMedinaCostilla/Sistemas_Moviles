package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesAdapter
import com.example.proyecto.models.Publicacion
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.network.ReaccionRequest
import com.example.proyecto.network.FavoritoRequest
import com.example.proyecto.utils.SessionManager
import com.example.proyecto.utils.NetworkUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicacionesAdapter
    private lateinit var btnHome: ImageView
    private lateinit var btnAdd: ImageView
    private lateinit var btnSearch: ImageView
    private lateinit var imgUserIcon: ImageView
    private lateinit var txtUsername: TextView

    private lateinit var layoutSinConexion: View
    private lateinit var progressBar: ProgressBar
    private lateinit var btnReintentar: Button

    private lateinit var sessionManager: SessionManager
    private var idUsuarioActual: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        layoutSinConexion = view.findViewById(R.id.layoutSinConexion)
        progressBar = view.findViewById(R.id.progressBar)
        btnReintentar = view.findViewById(R.id.btnReintentar)

        sessionManager = SessionManager(requireContext())

        // Obtener ID del usuario actual
        val userData = sessionManager.getUserData()
        idUsuarioActual = userData?.idUsuario ?: 0

        if (idUsuarioActual == 0) {
            Toast.makeText(context, "Error: No hay sesión activa", Toast.LENGTH_LONG).show()
            return
        }

        recyclerView = view.findViewById(R.id.recyclerViewPublicaciones)
        btnHome = view.findViewById(R.id.btnHome)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnSearch = view.findViewById(R.id.btnSearch)
        imgUserIcon = view.findViewById(R.id.imgUserIcon)
        txtUsername = view.findViewById(R.id.txtUsername)

        setupUserInfo()
        setupRecyclerView()
        setupBottomNavigation()
        setupTopNavigation()

        // Configurar botón de reintentar
        btnReintentar.setOnClickListener {
            verificarConexionYCargar()
        }

        // Verificar conexión y cargar
        verificarConexionYCargar()
    }

    private fun setupUserInfo() {
        val userData = sessionManager.getUserData()

        if (userData != null) {
            val nombreUsuario = sessionManager.getUser()
            txtUsername.text = nombreUsuario

            val fotoPerfil = userData.fotoPerfil

            if (!fotoPerfil.isNullOrEmpty()) {
                val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                val fullImageUrl = "$baseUrl$fotoPerfil"

                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(imgUserIcon)
            } else {
                imgUserIcon.setImageResource(R.drawable.user)
            }
        } else {
            txtUsername.text = "Usuario"
            imgUserIcon.setImageResource(R.drawable.user)
        }
    }

    private fun setupRecyclerView() {
        adapter = PublicacionesAdapter(
            publicaciones = emptyList(),
            onLikeClick = { publicacion ->
                manejarLike(publicacion)
            },
            onDislikeClick = { publicacion ->
                manejarDislike(publicacion)
            },
            onCommentClick = { publicacion ->
                println("🔍 DEBUG - Navegando a comentarios:")
                println("   ID Publicación: ${publicacion.id}")
                println("   Título: ${publicacion.titulo}")

                val bundle = Bundle().apply {
                    putInt("idPublicacion", publicacion.id.toInt())
                    putString("tituloPublicacion", publicacion.titulo)
                }

                println("   Bundle creado con keys: ${bundle.keySet().joinToString()}")

                findNavController().navigate(
                    R.id.action_homeFragment_to_commentsFragment,
                    bundle
                )
            },
            onFavoriteClick = { publicacion ->
                manejarFavorito(publicacion)
            },
            onPublicacionClick = { publicacion ->
                Toast.makeText(context, "Ver: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupBottomNavigation() {
        btnHome.setOnClickListener {
            Toast.makeText(context, "Home", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_agregarPublicacionFragment)
        }

        btnSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_busquedaFragment)
        }
    }

    private fun setupTopNavigation() {
        imgUserIcon.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_perfilFragment)
        }
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

                        val position = adapter.publicaciones.indexOf(publicacion)
                        if (position != -1) {
                            adapter.notifyItemChanged(position)
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

    // ==================== VERIFICACIÓN Y CARGA ====================

    private fun verificarConexionYCargar() {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            println("✅ Conexión disponible: ${NetworkUtils.getConnectionType(requireContext())}")
            ocultarMensajeSinConexion()
            cargarPublicaciones()
        } else {
            println("❌ Sin conexión a internet")
            mostrarMensajeSinConexion()
        }
    }

    private fun cargarPublicaciones() {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesApi.obtenerPublicaciones()

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")

                        val publicaciones = responseBody.data.map { pub ->
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
                        }

                        adapter.updatePublicaciones(publicaciones)
                        cargarEstadoReacciones(publicaciones)

                        mostrarCargando(false)
                        println("✅ ${publicaciones.size} publicaciones cargadas")
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
                if (!NetworkUtils.isInternetAvailable(requireContext())) {
                    mostrarMensajeSinConexion()
                } else {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recyclerView.visibility = if (mostrar) View.GONE else View.VISIBLE
    }

    private fun mostrarMensajeSinConexion() {
        layoutSinConexion.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.GONE

        Toast.makeText(
            context,
            "Sin conexión a internet",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun ocultarMensajeSinConexion() {
        layoutSinConexion.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
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
                    println("❌ Error al obtener estado de publicación ${pub.id}: ${e.message}")
                }
            }

            adapter.notifyDataSetChanged()
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
}