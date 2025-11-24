package com.example.proyecto.fragments

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesPerfilAdapter
import com.example.proyecto.models.Publicacion
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.SessionManager

class PerfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    // RecyclerView principal
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerAdapter: HeaderAdapter
    private lateinit var publicacionesAdapter: PublicacionesPerfilAdapter

    // Navegación inferior
    private lateinit var btnNavHome: ImageView
    private lateinit var btnNavAdd: ImageView
    private lateinit var btnNavSearch: ImageView

    // Estado de la pestaña activa
    private enum class TabActivo { PUBLICACIONES, FAVORITOS, BORRADORES }
    private var tabActual = TabActivo.PUBLICACIONES

    private var todasPublicaciones = mutableListOf<Publicacion>()
    private var publicacionesFavoritas = mutableSetOf<String>()
    private var publicacionesBorradores = mutableListOf<Publicacion>()
    private var idUsuarioActual: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        // Obtener ID del usuario actual
        val userData = sessionManager.getUserData()
        idUsuarioActual = userData?.idUsuario?.toString() ?: "0"

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
            idUsuarioActual = idUsuarioActual,
            onLikeClick = { publicacion ->
                Toast.makeText(context, "Like en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            },
            onDislikeClick = { publicacion ->
                Toast.makeText(context, "Dislike en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
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
                toggleFavorito(publicacion)
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
                val misPublicaciones = todasPublicaciones.filter { it.usuarioId == idUsuarioActual }
                mostrarPublicaciones(misPublicaciones)
            }
            TabActivo.FAVORITOS -> {
                headerAdapter.actualizarTitulo("Mis Favoritos")
                // Publicaciones favoritas (pueden ser de cualquier usuario)
                val favoritos = todasPublicaciones.filter { publicacionesFavoritas.contains(it.id) }
                mostrarPublicaciones(favoritos)
            }
            TabActivo.BORRADORES -> {
                headerAdapter.actualizarTitulo("Mis Borradores")
                // Borradores del usuario actual
                mostrarPublicaciones(publicacionesBorradores)
            }
        }
    }

    private fun cargarPublicaciones() {
        // Publicaciones publicadas
        todasPublicaciones = mutableListOf(
            Publicacion(
                id = "1",
                titulo = "Mi primera publicación",
                descripcion = "Esta publicación tiene 3 imágenes. Desliza para ver más.",
                imagenesUrl = listOf("gato1", "user", "star"),
                fecha = "20 de nov. 2025, 10:00 AM",
                likes = 45,
                dislikes = 3,
                comentarios = 12,
                favoritos = 5,
                usuarioId = idUsuarioActual,
                usuarioNombre = "Tú",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            ),
            Publicacion(
                id = "2",
                titulo = "Tutorial de Android",
                descripcion = "Aprende a crear apps increíbles con Kotlin.",
                imagenesUrl = listOf("home", "buscar"),
                fecha = "21 de nov. 2025, 2:30 PM",
                likes = 67,
                dislikes = 5,
                comentarios = 23,
                favoritos = 8,
                usuarioId = "999",  // Publicación de otro usuario
                usuarioNombre = "Juan Pérez",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            ),
            Publicacion(
                id = "3",
                titulo = "Jetpack Compose",
                descripcion = "Construye UIs modernas y declarativas.",
                imagenesUrl = listOf("gato1"),
                fecha = "21 de nov. 2025, 5:15 PM",
                likes = 34,
                dislikes = 2,
                comentarios = 8,
                favoritos = 3,
                usuarioId = idUsuarioActual,
                usuarioNombre = "Tú",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            ),
            Publicacion(
                id = "4",
                titulo = "MVVM Architecture",
                descripcion = "Implementa el patrón MVVM en tus apps Android.",
                imagenesUrl = listOf("add", "star", "like"),
                fecha = "22 de nov. 2025, 9:00 AM",
                likes = 89,
                dislikes = 7,
                comentarios = 31,
                favoritos = 12,
                usuarioId = "888",  // Publicación de otro usuario
                usuarioNombre = "María López",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            )
        )

        // Borradores (solo del usuario actual)
        publicacionesBorradores = mutableListOf(
            Publicacion(
                id = "draft_1",
                titulo = "Guía de Kotlin Coroutines",
                descripcion = "Aprende a manejar operaciones asíncronas de forma eficiente. Este borrador necesita más detalles...",
                imagenesUrl = listOf("gato1", "home"),
                fecha = "Borrador guardado el 19 de nov. 2025",
                likes = 0,
                dislikes = 0,
                comentarios = 0,
                favoritos = 0,
                usuarioId = idUsuarioActual,
                usuarioNombre = "Tú",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            ),
            Publicacion(
                id = "draft_2",
                titulo = "Room Database Tutorial",
                descripcion = "Persistencia local en Android con Room. [Incompleto]",
                imagenesUrl = listOf("star"),
                fecha = "Borrador guardado el 18 de nov. 2025",
                likes = 0,
                dislikes = 0,
                comentarios = 0,
                favoritos = 0,
                usuarioId = idUsuarioActual,
                usuarioNombre = "Tú",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            ),
            Publicacion(
                id = "draft_3",
                titulo = "Diseño Material 3",
                descripcion = "Implementa Material You en tus aplicaciones Android...",
                imagenesUrl = listOf("add", "buscar", "chat"),
                fecha = "Borrador guardado el 17 de nov. 2025",
                likes = 0,
                dislikes = 0,
                comentarios = 0,
                favoritos = 0,
                usuarioId = idUsuarioActual,
                usuarioNombre = "Tú",
                usuarioLike = false,
                usuarioDislike = false,
                usuarioFavorito = false
            )
        )

        publicacionesFavoritas.addAll(listOf("2", "4"))  // Las de otros usuarios están en favoritos
        seleccionarTab(TabActivo.PUBLICACIONES)
    }

    private fun mostrarPublicaciones(publicaciones: List<Publicacion>) {
        publicacionesAdapter.updatePublicaciones(publicaciones)
    }

    private fun toggleFavorito(publicacion: Publicacion) {
        if (publicacionesFavoritas.contains(publicacion.id)) {
            publicacionesFavoritas.remove(publicacion.id)
            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
        } else {
            publicacionesFavoritas.add(publicacion.id)
            Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
        }

        if (tabActual == TabActivo.FAVORITOS) {
            seleccionarTab(TabActivo.FAVORITOS)
        }
    }

    private fun eliminarPublicacion(publicacion: Publicacion) {
        // Determinar si es borrador o publicación
        val esBorrador = publicacion.id.startsWith("draft_")

        if (esBorrador) {
            // Eliminar de borradores
            publicacionesBorradores.removeAll { it.id == publicacion.id }
            Toast.makeText(
                context,
                "Borrador \"${publicacion.titulo}\" eliminado",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Eliminar de publicaciones
            todasPublicaciones.removeAll { it.id == publicacion.id }
            publicacionesFavoritas.remove(publicacion.id)
            Toast.makeText(
                context,
                "\"${publicacion.titulo}\" eliminada correctamente",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Actualizar vista
        seleccionarTab(tabActual)

        // TODO: Aquí harías la llamada al API para eliminar del servidor
        // lifecycleScope.launch {
        //     try {
        //         val endpoint = if (esBorrador) "borradores" else "publicaciones"
        //         val response = RetrofitClient.api.eliminar(endpoint, publicacion.id.toInt())
        //         if (response.isSuccessful) {
        //             // Ya se eliminó localmente
        //         }
        //     } catch (e: Exception) {
        //         Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
        //     }
        // }
    }

    // Adapter para el header (sin cambios)
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