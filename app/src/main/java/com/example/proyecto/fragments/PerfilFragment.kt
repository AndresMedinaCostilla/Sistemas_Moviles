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
import com.example.proyecto.adapters.PublicacionesAdapter
import com.example.proyecto.models.Publicacion
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.SessionManager

class PerfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    // RecyclerView principal
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerAdapter: HeaderAdapter
    private lateinit var publicacionesAdapter: PublicacionesAdapter

    // Navegación inferior
    private lateinit var btnNavHome: ImageView
    private lateinit var btnNavAdd: ImageView
    private lateinit var btnNavSearch: ImageView

    // Estado de la pestaña activa
    private enum class TabActivo { PUBLICACIONES, FAVORITOS, BORRADORES }
    private var tabActual = TabActivo.PUBLICACIONES

    private var todasPublicaciones = listOf<Publicacion>()
    private var publicacionesFavoritas = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

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

        // Crear publicaciones adapter
        publicacionesAdapter = PublicacionesAdapter(
            publicaciones = emptyList(),
            onLikeClick = { publicacion ->
                Toast.makeText(context, "Like en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            },
            onDislikeClick = { publicacion ->
                Toast.makeText(context, "Dislike en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            },
            onCommentClick = { publicacion ->
                findNavController().navigate(R.id.action_perfilFragment_to_commentsFragment)
            },
            onFavoriteClick = { publicacion ->
                toggleFavorito(publicacion)
            },
            onPublicacionClick = { publicacion ->
                Toast.makeText(context, "Ver: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
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
                mostrarPublicaciones(todasPublicaciones)
            }
            TabActivo.FAVORITOS -> {
                headerAdapter.actualizarTitulo("Mis Favoritos")
                val favoritos = todasPublicaciones.filter { publicacionesFavoritas.contains(it.id) }
                mostrarPublicaciones(favoritos)
            }
            TabActivo.BORRADORES -> {
                headerAdapter.actualizarTitulo("Mis Borradores")
                mostrarPublicaciones(emptyList())
            }
        }
    }

    private fun cargarPublicaciones() {
        todasPublicaciones = listOf(
            Publicacion(
                id = "1",
                titulo = "Mi primera publicación",
                descripcion = "Esta publicación tiene 3 imágenes. Desliza para ver más.",
                imagenesUrl = listOf("gato1", "user", "star"),
                fecha = "20 de nov. 2025, 10:00 AM",
                likes = 45,
                dislikes = 3,
                comentarios = 12,
                usuarioId = "user1",
                usuarioNombre = "Tú"
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
                usuarioId = "user1",
                usuarioNombre = "Tú"
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
                usuarioId = "user1",
                usuarioNombre = "Tú"
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
                usuarioId = "user1",
                usuarioNombre = "Tú"
            )
        )

        publicacionesFavoritas.addAll(listOf("2", "4"))
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

    // Adapter para el header
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
            // Cargar datos del usuario
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

            // Configurar listeners
            holder.btnEditarPerfil.setOnClickListener { onEditarClick() }
            holder.btnCerrarSesion.setOnClickListener { onCerrarSesionClick() }

            holder.btnPublicaciones.setOnClickListener { onTabClick(0) }
            holder.btnFavoritos.setOnClickListener { onTabClick(1) }
            holder.btnBorradores.setOnClickListener { onTabClick(2) }

            // Actualizar estado de tabs
            actualizarEstiloTabs(holder)

            // Actualizar título
            holder.tvTituloSeccion.text = tituloSeccion
        }

        private fun actualizarEstiloTabs(holder: HeaderViewHolder) {
            // Reset todos
            resetearTab(holder.iconPublicaciones, holder.textPublicaciones)
            resetearTab(holder.iconFavoritos, holder.textFavoritos)
            resetearTab(holder.iconBorradores, holder.textBorradores)

            // Activar seleccionado
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