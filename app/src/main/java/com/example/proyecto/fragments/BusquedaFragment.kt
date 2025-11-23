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
    private var publicacionesFavoritas = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_busqueda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etBuscar = view.findViewById(R.id.etBuscar)
        btnToggleFavoritos = view.findViewById(R.id.btnToggleFavoritos)
        txtFiltroActivo = view.findViewById(R.id.txtFiltroActivo)
        recyclerResultados = view.findViewById(R.id.recyclerResultados)
        layoutNoResultados = view.findViewById(R.id.layoutNoResultados)
        txtNoResultados = view.findViewById(R.id.txtNoResultados)

        setupRecyclerView()
        setupBusqueda()
        setupToggleFavoritos()
        cargarPublicaciones()
        actualizarResultados("")
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
                findNavController().navigate(R.id.action_busquedaFragment_to_commentsFragment)
            },
            onFavoriteClick = { publicacion ->
                toggleFavorito(publicacion)
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

    private fun actualizarResultados(query: String) {
        val publicacionesBase = if (buscarSoloFavoritos) {
            todasLasPublicaciones.filter { publicacionesFavoritas.contains(it.id) }
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
            recyclerResultados.visibility = View.VISIBLE
            layoutNoResultados.visibility = View.GONE
            adapter.updatePublicaciones(resultados)
        }
    }

    private fun toggleFavorito(publicacion: Publicacion) {
        if (publicacionesFavoritas.contains(publicacion.id)) {
            publicacionesFavoritas.remove(publicacion.id)
            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
        } else {
            publicacionesFavoritas.add(publicacion.id)
            Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
        }

        if (buscarSoloFavoritos) {
            actualizarResultados(etBuscar.text.toString())
        }
    }

    private fun cargarPublicaciones() {
        // DATOS DE EJEMPLO con múltiples imágenes
        todasLasPublicaciones = listOf(
            Publicacion(
                id = "1",
                titulo = "Introducción a Kotlin",
                descripcion = "Aprende los fundamentos de Kotlin para desarrollo Android.",
                imagenesUrl = listOf("gato1", "user", "star"),
                fecha = "20 de nov. 2025, 10:00 AM",
                likes = 45,
                dislikes = 3,
                comentarios = 12,
                usuarioId = "user1",
                usuarioNombre = "Ana Desarrolladora"
            ),
            Publicacion(
                id = "2",
                titulo = "Jetpack Compose Tutorial",
                descripcion = "Construye interfaces modernas con Jetpack Compose.",
                imagenesUrl = listOf("home", "buscar"),
                fecha = "21 de nov. 2025, 2:30 PM",
                likes = 67,
                dislikes = 5,
                comentarios = 23,
                usuarioId = "user2",
                usuarioNombre = "Carlos Tech"
            ),
            Publicacion(
                id = "3",
                titulo = "Firebase y Android",
                descripcion = "Integra Firebase en tu aplicación Android.",
                imagenesUrl = listOf("gato1"),
                fecha = "21 de nov. 2025, 5:15 PM",
                likes = 34,
                dislikes = 2,
                comentarios = 8,
                usuarioId = "user3",
                usuarioNombre = "María Backend"
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
                usuarioId = "user4",
                usuarioNombre = "Luis Arquitecto"
            ),
            Publicacion(
                id = "5",
                titulo = "Room Database Tutorial",
                descripcion = "Persiste datos localmente con Room.",
                imagenesUrl = listOf("chat", "add"),
                fecha = "22 de nov. 2025, 11:45 AM",
                likes = 52,
                dislikes = 4,
                comentarios = 15,
                usuarioId = "user1",
                usuarioNombre = "Ana Desarrolladora"
            ),
            Publicacion(
                id = "6",
                titulo = "Retrofit y API REST",
                descripcion = "Conecta tu app con APIs REST usando Retrofit.",
                imagenesUrl = listOf("gato1", "user", "home"),
                fecha = "22 de nov. 2025, 3:20 PM",
                likes = 78,
                dislikes = 6,
                comentarios = 19,
                usuarioId = "user5",
                usuarioNombre = "Pedro Network"
            )
        )

        publicacionesFavoritas.addAll(listOf("2", "4"))
    }
}