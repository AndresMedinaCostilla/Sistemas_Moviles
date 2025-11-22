package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesAdapter
import com.example.proyecto.models.Publicacion

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicacionesAdapter
    private lateinit var btnHome: ImageView
    private lateinit var btnAdd: ImageView
    private lateinit var btnSearch: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        recyclerView = view.findViewById(R.id.recyclerViewPublicaciones)
        btnHome = view.findViewById(R.id.btnHome)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnSearch = view.findViewById(R.id.btnSearch)

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar listeners de navegación
        setupBottomNavigation()

        // Cargar publicaciones (datos de ejemplo)
        cargarPublicaciones()
    }

    private fun setupRecyclerView() {
        adapter = PublicacionesAdapter(
            publicaciones = emptyList(),
            onLikeClick = { publicacion ->
                // Manejar like
                Toast.makeText(context, "Like en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
                // TODO: Actualizar en la base de datos
            },
            onCommentClick = { publicacion ->
                // Navegar a comentarios
                findNavController().navigate(R.id.action_homeFragment_to_commentsFragment)
                // TODO: Pasar el ID de la publicación como argumento
                // val bundle = Bundle().apply { putString("publicacionId", publicacion.id) }
                // findNavController().navigate(R.id.action_homeFragment_to_commentsFragment, bundle)
            },
            onFavoriteClick = { publicacion ->
                // Manejar favorito
                Toast.makeText(context, "Favorito: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
                // TODO: Actualizar en la base de datos
            },
            onPublicacionClick = { publicacion ->
                // Ver detalle de publicación
                Toast.makeText(context, "Ver: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
                // TODO: Navegar a detalle de publicación
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupBottomNavigation() {
        btnHome.setOnClickListener {
            // Ya estamos en home
            Toast.makeText(context, "Home", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            // Navegar a agregar publicación
            findNavController().navigate(R.id.action_homeFragment_to_agregarPublicacionFragment)
        }

        btnSearch.setOnClickListener {
            // Navegar a búsqueda
            findNavController().navigate(R.id.action_homeFragment_to_busquedaFragment)
        }
    }

    private fun cargarPublicaciones() {
        // DATOS DE EJEMPLO - Reemplaza esto con tu consulta a la base de datos
        val publicacionesEjemplo = listOf(
            Publicacion(
                id = "1",
                titulo = "Mi primera publicación",
                descripcion = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore.",
                imagenUrl = "",
                fecha = "Creado el 20 de septiembre del 2025 a las 12:00 pm",
                likes = 15,
                comentarios = 3,
                usuarioId = "user1",
                usuarioNombre = "Usuario1"
            ),
            Publicacion(
                id = "2",
                titulo = "Segunda publicación increíble",
                descripcion = "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo.",
                imagenUrl = "",
                fecha = "Creado el 21 de septiembre del 2025 a las 3:30 pm",
                likes = 42,
                comentarios = 8,
                usuarioId = "user2",
                usuarioNombre = "Usuario2"
            ),
            Publicacion(
                id = "3",
                titulo = "Explorando nuevas tecnologías",
                descripcion = "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
                imagenUrl = "",
                fecha = "Creado el 22 de septiembre del 2025 a las 9:15 am",
                likes = 28,
                comentarios = 12,
                usuarioId = "user3",
                usuarioNombre = "Usuario3"
            )
        )

        // Actualizar adapter con las publicaciones
        adapter.updatePublicaciones(publicacionesEjemplo)

        // TODO: Cuando conectes a tu base de datos, reemplaza esto por:
        /*
        viewModel.obtenerPublicaciones().observe(viewLifecycleOwner) { publicaciones ->
            adapter.updatePublicaciones(publicaciones)
        }
        */
    }
}