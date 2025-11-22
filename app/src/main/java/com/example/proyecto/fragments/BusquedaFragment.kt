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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.PublicacionesAdapter
import com.example.proyecto.models.Publicacion

class BusquedaFragment : Fragment() {

    private lateinit var etBuscar: EditText
    private lateinit var btnToggleFavoritos: ImageView
    private lateinit var txtFiltroActivo: TextView
    private lateinit var recyclerResultados: RecyclerView
    private lateinit var layoutNoResultados: LinearLayout
    private lateinit var txtNoResultados: TextView
    private lateinit var adapter: PublicacionesAdapter

    private var buscarSoloFavoritos = false
    private var todasLasPublicaciones = listOf<Publicacion>()
    private var publicacionesFavoritas = mutableSetOf<String>() // IDs de favoritos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_busqueda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        etBuscar = view.findViewById(R.id.etBuscar)
        btnToggleFavoritos = view.findViewById(R.id.btnToggleFavoritos)
        txtFiltroActivo = view.findViewById(R.id.txtFiltroActivo)
        recyclerResultados = view.findViewById(R.id.recyclerResultados)
        layoutNoResultados = view.findViewById(R.id.layoutNoResultados)
        txtNoResultados = view.findViewById(R.id.txtNoResultados)

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar búsqueda
        setupBusqueda()

        // Configurar toggle de favoritos
        setupToggleFavoritos()

        // Cargar publicaciones de ejemplo
        cargarPublicaciones()

        // Mostrar todas las publicaciones al inicio
        actualizarResultados("")
    }

    private fun setupRecyclerView() {
        adapter = PublicacionesAdapter(
            publicaciones = emptyList(),
            onLikeClick = { publicacion ->
                Toast.makeText(context, "Like en: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
                // TODO: Actualizar en la base de datos
            },
            onCommentClick = { publicacion ->
                // Navegar a comentarios
                findNavController().navigate(R.id.action_busquedaFragment_to_commentsFragment)
                // TODO: Pasar el ID de la publicación como argumento
            },
            onFavoriteClick = { publicacion ->
                toggleFavorito(publicacion)
            },
            onPublicacionClick = { publicacion ->
                Toast.makeText(context, "Ver: ${publicacion.titulo}", Toast.LENGTH_SHORT).show()
                // TODO: Navegar a detalle de publicación
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
            // Estado ACTIVADO - Buscar solo en favoritos
            btnToggleFavoritos.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
            txtFiltroActivo.text = "Buscando en: Solo favoritos ⭐"
            txtFiltroActivo.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
        } else {
            // Estado DESACTIVADO - Buscar en todas
            btnToggleFavoritos.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
            txtFiltroActivo.text = "Buscando en: Todas las publicaciones"
            txtFiltroActivo.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
        }
    }

    private fun actualizarResultados(query: String) {
        val publicacionesBase = if (buscarSoloFavoritos) {
            // Filtrar solo favoritos
            todasLasPublicaciones.filter { publicacionesFavoritas.contains(it.id) }
        } else {
            // Todas las publicaciones
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
            // Mostrar mensaje de no resultados
            recyclerResultados.visibility = View.GONE
            layoutNoResultados.visibility = View.VISIBLE

            txtNoResultados.text = when {
                buscarSoloFavoritos && publicacionesFavoritas.isEmpty() ->
                    "No tienes publicaciones en favoritos"
                buscarSoloFavoritos ->
                    "No se encontraron resultados en favoritos"
                query.isEmpty() ->
                    "No hay publicaciones disponibles"
                else ->
                    "No se encontraron resultados para \"$query\""
            }
        } else {
            // Mostrar resultados
            recyclerResultados.visibility = View.VISIBLE
            layoutNoResultados.visibility = View.GONE
            adapter.updatePublicaciones(resultados)
        }
    }

    private fun toggleFavorito(publicacion: Publicacion) {
        if (publicacionesFavoritas.contains(publicacion.id)) {
            // Quitar de favoritos
            publicacionesFavoritas.remove(publicacion.id)
            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
        } else {
            // Agregar a favoritos
            publicacionesFavoritas.add(publicacion.id)
            Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
        }

        // Actualizar resultados si estamos filtrando por favoritos
        if (buscarSoloFavoritos) {
            actualizarResultados(etBuscar.text.toString())
        }

        // TODO: Actualizar en la base de datos
    }

    private fun cargarPublicaciones() {
        // DATOS DE EJEMPLO - Reemplaza con tu consulta a la base de datos
        todasLasPublicaciones = listOf(
            Publicacion(
                id = "1",
                titulo = "Introducción a Kotlin",
                descripcion = "Aprende los fundamentos de Kotlin para desarrollo Android. Un lenguaje moderno y expresivo.",
                imagenUrl = "",
                fecha = "20 de nov. 2025, 10:00 AM",
                likes = 45,
                comentarios = 12,
                usuarioId = "user1",
                usuarioNombre = "Ana Desarrolladora"
            ),
            Publicacion(
                id = "2",
                titulo = "Jetpack Compose Tutorial",
                descripcion = "Construye interfaces modernas con Jetpack Compose. El futuro del desarrollo UI en Android.",
                imagenUrl = "",
                fecha = "21 de nov. 2025, 2:30 PM",
                likes = 67,
                comentarios = 23,
                usuarioId = "user2",
                usuarioNombre = "Carlos Tech"
            ),
            Publicacion(
                id = "3",
                titulo = "Firebase y Android",
                descripcion = "Integra Firebase en tu aplicación Android: Auth, Firestore, Storage y más.",
                imagenUrl = "",
                fecha = "21 de nov. 2025, 5:15 PM",
                likes = 34,
                comentarios = 8,
                usuarioId = "user3",
                usuarioNombre = "María Backend"
            ),
            Publicacion(
                id = "4",
                titulo = "MVVM Architecture",
                descripcion = "Implementa el patrón MVVM en tus apps Android para código más limpio y mantenible.",
                imagenUrl = "",
                fecha = "22 de nov. 2025, 9:00 AM",
                likes = 89,
                comentarios = 31,
                usuarioId = "user4",
                usuarioNombre = "Luis Arquitecto"
            ),
            Publicacion(
                id = "5",
                titulo = "Room Database Tutorial",
                descripcion = "Persiste datos localmente con Room, la biblioteca de persistencia de Android.",
                imagenUrl = "",
                fecha = "22 de nov. 2025, 11:45 AM",
                likes = 52,
                comentarios = 15,
                usuarioId = "user1",
                usuarioNombre = "Ana Desarrolladora"
            ),
            Publicacion(
                id = "6",
                titulo = "Retrofit y API REST",
                descripcion = "Conecta tu app con APIs REST usando Retrofit. Aprende a consumir servicios web.",
                imagenUrl = "",
                fecha = "22 de nov. 2025, 3:20 PM",
                likes = 78,
                comentarios = 19,
                usuarioId = "user5",
                usuarioNombre = "Pedro Network"
            )
        )

        // Simular algunos favoritos iniciales (opcional)
        publicacionesFavoritas.addAll(listOf("2", "4"))

        // TODO: Cuando conectes a tu base de datos:
        /*
        viewModel.obtenerPublicaciones().observe(viewLifecycleOwner) { publicaciones ->
            todasLasPublicaciones = publicaciones
            actualizarResultados(etBuscar.text.toString())
        }

        viewModel.obtenerFavoritos().observe(viewLifecycleOwner) { favoritos ->
            publicacionesFavoritas = favoritos.toMutableSet()
        }
        */
    }
}