package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesAdapter
import com.example.proyecto.models.Publicacion
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.SessionManager

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicacionesAdapter
    private lateinit var btnHome: ImageView
    private lateinit var btnAdd: ImageView
    private lateinit var btnSearch: ImageView
    private lateinit var imgUserIcon: ImageView
    private lateinit var txtUsername: TextView

    // SessionManager para obtener datos del usuario
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar SessionManager
        sessionManager = SessionManager(requireContext())

        // Inicializar vistas
        recyclerView = view.findViewById(R.id.recyclerViewPublicaciones)
        btnHome = view.findViewById(R.id.btnHome)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnSearch = view.findViewById(R.id.btnSearch)
        imgUserIcon = view.findViewById(R.id.imgUserIcon)
        txtUsername = view.findViewById(R.id.txtUsername)

        // 🔥 Configurar información del usuario desde la sesión
        setupUserInfo()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar listeners de navegación
        setupBottomNavigation()
        setupTopNavigation()

        // Cargar publicaciones (datos de ejemplo)
        cargarPublicaciones()
    }

    /**
     * Configura la información del usuario en el toolbar superior
     */
    private fun setupUserInfo() {
        val userData = sessionManager.getUserData()

        if (userData != null) {
            // Mostrar nombre de usuario
            val nombreUsuario = sessionManager.getUser()
            txtUsername.text = nombreUsuario

            // 🔍 LOGS DE DEBUG
            println("🔍 DEBUG - userData completo: $userData")
            println("🔍 DEBUG - fotoPerfil value: '${userData.fotoPerfil}'")
            println("🔍 DEBUG - fotoPerfil isEmpty: ${userData.fotoPerfil?.isEmpty()}")
            println("🔍 DEBUG - fotoPerfil isNullOrEmpty: ${userData.fotoPerfil.isNullOrEmpty()}")

            val fotoPerfil = userData.fotoPerfil

            if (!fotoPerfil.isNullOrEmpty()) {
                val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                val fullImageUrl = "$baseUrl$fotoPerfil"

                println("🖼️ URL completa a cargar: $fullImageUrl")

                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            println("❌ Glide error al cargar imagen")
                            println("❌ Excepción: ${e?.message}")
                            e?.rootCauses?.forEach { cause ->
                                println("❌ Causa: ${cause.message}")
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            println("✅ Imagen cargada exitosamente desde: $dataSource")
                            return false
                        }
                    })
                    .into(imgUserIcon)
            } else {
                println("⚠️ fotoPerfil está vacío o es null")
                imgUserIcon.setImageResource(R.drawable.user)
            }
        } else {
            println("❌ userData es null")
            txtUsername.text = "Usuario"
            imgUserIcon.setImageResource(R.drawable.user)
            Toast.makeText(context, "No hay sesión activa", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = PublicacionesAdapter(
            publicaciones = emptyList(),
            onLikeClick = { publicacion ->
                Toast.makeText(context, "Like en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            },
            onDislikeClick = { publicacion ->
                Toast.makeText(context, "Dislike en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
            },
            onCommentClick = { publicacion ->
                findNavController().navigate(R.id.action_homeFragment_to_commentsFragment)
            },
            onFavoriteClick = { publicacion ->
                Toast.makeText(context, "Favorito: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Perfil", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarPublicaciones() {
        // DATOS DE EJEMPLO
        val publicacionesEjemplo = listOf(
            Publicacion(
                id = "1",
                titulo = "Mi primera publicación",
                descripcion = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                imagenUrl = "",
                fecha = "Creado el 20 de septiembre del 2025 a las 12:00 pm",
                likes = 15,
                dislikes = 2,
                comentarios = 3,
                usuarioId = "user1",
                usuarioNombre = "Usuario1"
            ),
            Publicacion(
                id = "2",
                titulo = "Segunda publicación increíble",
                descripcion = "Ut enim ad minim veniam, quis nostrud exercitation.",
                imagenUrl = "",
                fecha = "Creado el 21 de septiembre del 2025 a las 3:30 pm",
                likes = 42,
                dislikes = 5,
                comentarios = 8,
                usuarioId = "user2",
                usuarioNombre = "Usuario2"
            ),
            Publicacion(
                id = "3",
                titulo = "Explorando nuevas tecnologías",
                descripcion = "Duis aute irure dolor in reprehenderit in voluptate.",
                imagenUrl = "",
                fecha = "Creado el 22 de septiembre del 2025 a las 9:15 am",
                likes = 28,
                dislikes = 1,
                comentarios = 12,
                usuarioId = "user3",
                usuarioNombre = "Usuario3"
            )
        )

        adapter.updatePublicaciones(publicacionesEjemplo)
    }
}