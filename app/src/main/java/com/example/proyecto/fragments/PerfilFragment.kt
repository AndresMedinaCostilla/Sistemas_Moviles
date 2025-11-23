package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.SessionManager

class PerfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    // Vistas del perfil
    private lateinit var imgFotoPerfil: ImageView
    private lateinit var txtNombre: TextView
    private lateinit var txtUsuario: TextView
    private lateinit var txtMiembroDesde: TextView
    private lateinit var btnCerrarSesion: Button

    // Botones de navegación inferior
    private lateinit var btnHome: ImageView
    private lateinit var btnAdd: ImageView
    private lateinit var btnSearch: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar SessionManager
        sessionManager = SessionManager(requireContext())

        // Verificar si hay sesión activa
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(context, "No hay sesión activa", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
            return
        }

        // Inicializar vistas
        inicializarVistas(view)

        // Cargar datos del usuario
        cargarDatosUsuario()

        // Configurar botón de cerrar sesión
        setupCerrarSesion()

        // Configurar navegación inferior
        setupBottomNavigation()
    }

    private fun inicializarVistas(view: View) {
        // Vistas del perfil (encuentra los IDs correctos en tu XML)
        imgFotoPerfil = view.findViewById(R.id.imgPerfil) ?: view.findViewById<ImageView>(
            view.context.resources.getIdentifier("imgFotoPerfil", "id", view.context.packageName)
        )

        // Buscar los TextViews por el texto que tienen por defecto
        txtNombre = view.findViewById<TextView>(
            view.context.resources.getIdentifier("txtNombre", "id", view.context.packageName)
        ) ?: buscarTextViewPorTexto(view, "Nombre")

        txtUsuario = view.findViewById<TextView>(
            view.context.resources.getIdentifier("txtUsuario", "id", view.context.packageName)
        ) ?: buscarTextViewPorTexto(view, "@usuario")

        txtMiembroDesde = view.findViewById<TextView>(
            view.context.resources.getIdentifier("txtMiembroDesde", "id", view.context.packageName)
        ) ?: buscarTextViewPorTexto(view, "Miembro desde")

        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion)

        // Botones de navegación
        val bottomNav = view.findViewById<View>(R.id.bottomNav)
        val imageViews = obtenerImageViews(bottomNav)

        if (imageViews.size >= 3) {
            btnHome = imageViews[0]
            btnAdd = imageViews[1]
            btnSearch = imageViews[2]
        }
    }

    private fun buscarTextViewPorTexto(view: View, textoDefecto: String): TextView {
        // Función auxiliar para buscar TextViews por su texto por defecto
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is TextView && child.text.toString().contains(textoDefecto, ignoreCase = true)) {
                    return child
                }
                if (child is ViewGroup) {
                    val found = buscarTextViewPorTexto(child, textoDefecto)
                    if (found.text.isNotEmpty()) return found
                }
            }
        }
        return TextView(requireContext()) // Retorna un TextView vacío si no encuentra
    }

    private fun obtenerImageViews(view: View): List<ImageView> {
        val imageViews = mutableListOf<ImageView>()
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is ImageView) {
                    imageViews.add(child)
                } else if (child is ViewGroup) {
                    imageViews.addAll(obtenerImageViews(child))
                }
            }
        }
        return imageViews
    }

    private fun cargarDatosUsuario() {
        val userData = sessionManager.getUserData()

        if (userData != null) {
            // Mostrar nombre completo
            val nombreCompleto = sessionManager.getNombreCompleto()
            txtNombre.text = nombreCompleto

            // Mostrar nombre de usuario
            txtUsuario.text = "@${userData.usuario}"

            // Mostrar fecha de registro
            val fechaRegistro = userData.fechaRegistro ?: "2024"
            val anio = fechaRegistro.substring(0, 4) // Extraer el año
            txtMiembroDesde.text = "Miembro desde $anio"

            // Cargar foto de perfil
            val fotoPerfil = userData.fotoPerfil
            if (!fotoPerfil.isNullOrEmpty()) {
                val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                val fullImageUrl = "$baseUrl$fotoPerfil"

                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(imgFotoPerfil)

                println("🖼️ Cargando foto de perfil: $fullImageUrl")
            } else {
                imgFotoPerfil.setImageResource(R.drawable.user)
            }

            // Log de debug
            println("👤 Perfil cargado: ${userData.nombre} ${userData.apellidoPaterno}")
        } else {
            Toast.makeText(context, "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCerrarSesion() {
        btnCerrarSesion.setOnClickListener {
            // Mostrar diálogo de confirmación
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    // Cerrar sesión
                    sessionManager.logout()

                    Toast.makeText(
                        context,
                        "Sesión cerrada exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navegar al login
                    findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun setupBottomNavigation() {
        try {
            btnHome.setOnClickListener {
                findNavController().navigate(R.id.action_perfilFragment_to_homeFragment)
            }

            btnAdd.setOnClickListener {
                findNavController().navigate(R.id.action_perfilFragment_to_agregarPublicacionFragment)
            }

            btnSearch.setOnClickListener {
                Toast.makeText(context, "Búsqueda", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            println("⚠️ Error configurando navegación: ${e.message}")
        }
    }
}