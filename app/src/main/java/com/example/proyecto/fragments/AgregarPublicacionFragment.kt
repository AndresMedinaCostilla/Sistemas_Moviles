package com.example.proyecto.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.ImagenesAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AgregarPublicacionFragment : Fragment() {

    private lateinit var btnCancelar: ImageButton
    private lateinit var btnGaleria: ImageButton
    private lateinit var btnPublicar: Button
    private lateinit var etTitulo: EditText
    private lateinit var etContenido: EditText
    private lateinit var tvContadorCaracteres: TextView
    private lateinit var tvContadorImagenes: TextView
    private lateinit var layoutImagenes: CardView
    private lateinit var recyclerImagenes: RecyclerView

    private lateinit var imagenesAdapter: ImagenesAdapter
    private val imagenesSeleccionadas = mutableListOf<Uri>()
    private var fotoUri: Uri? = null

    // ActivityResultLauncher para la galería
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                if (imagenesSeleccionadas.size < 3) {
                    imagenesAdapter.agregarImagen(uri)
                }
            }
            actualizarVistaImagenes()
        }
    }

    // ActivityResultLauncher para la cámara
    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && fotoUri != null) {
            if (imagenesSeleccionadas.size < 3) {
                imagenesAdapter.agregarImagen(fotoUri!!)
                actualizarVistaImagenes()
            }
        }
    }

    // Launcher para permisos de cámara
    private val permisosCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            abrirCamara()
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para permisos de galería
    private val permisosGaleriaLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            abrirGaleria()
        } else {
            Toast.makeText(context, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_agregar_publicacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)
        configurarRecyclerView()
        configurarListeners()
        configurarContadorCaracteres()
    }

    private fun inicializarVistas(view: View) {
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnGaleria = view.findViewById(R.id.btnGaleria)
        btnPublicar = view.findViewById(R.id.btnPublicar)
        etTitulo = view.findViewById(R.id.etTitulo)
        etContenido = view.findViewById(R.id.etContenido)
        tvContadorCaracteres = view.findViewById(R.id.tvContadorCaracteres)
        tvContadorImagenes = view.findViewById(R.id.tvContadorImagenes)
        layoutImagenes = view.findViewById(R.id.layoutImagenes)
        recyclerImagenes = view.findViewById(R.id.recyclerImagenes)
    }

    private fun configurarRecyclerView() {
        imagenesAdapter = ImagenesAdapter(imagenesSeleccionadas) { position ->
            // Eliminar imagen
            imagenesAdapter.eliminarImagen(position)
            actualizarVistaImagenes()
        }

        recyclerImagenes.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerImagenes.adapter = imagenesAdapter
    }

    private fun configurarListeners() {
        // Botón cancelar
        btnCancelar.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón galería - Mostrar diálogo de opciones
        btnGaleria.setOnClickListener {
            if (imagenesSeleccionadas.size < 3) {
                mostrarDialogoOpciones()
            } else {
                Toast.makeText(context, "Máximo 3 imágenes permitidas", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón publicar
        btnPublicar.setOnClickListener {
            publicarContenido()
        }
    }

    private fun configurarContadorCaracteres() {
        etContenido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvContadorCaracteres.text = "$length/500"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun mostrarDialogoOpciones() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> verificarYAbrirCamara()
                    1 -> verificarYAbrirGaleria()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verificarYAbrirCamara() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara()
            }
            else -> {
                permisosCamaraLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun verificarYAbrirGaleria() {
        val permiso = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permiso) == PackageManager.PERMISSION_GRANTED -> {
                abrirGaleria()
            }
            else -> {
                permisosGaleriaLauncher.launch(permiso)
            }
        }
    }

    private fun abrirCamara() {
        try {
            val fotoFile = crearArchivoImagen()
            fotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                fotoFile
            )
            camaraLauncher.launch(fotoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirGaleria() {
        val cantidadRestante = 3 - imagenesSeleccionadas.size
        if (cantidadRestante > 0) {
            galeriaLauncher.launch("image/*")
        }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun actualizarVistaImagenes() {
        if (imagenesSeleccionadas.isEmpty()) {
            layoutImagenes.visibility = View.GONE
        } else {
            layoutImagenes.visibility = View.VISIBLE
        }
        tvContadorImagenes.text = "${imagenesSeleccionadas.size}/3 imágenes"
    }

    private fun publicarContenido() {
        val titulo = etTitulo.text.toString().trim()
        val contenido = etContenido.text.toString().trim()

        when {
            titulo.isEmpty() -> {
                Toast.makeText(context, "Agrega un título", Toast.LENGTH_SHORT).show()
            }
            contenido.isEmpty() -> {
                Toast.makeText(context, "Agrega contenido", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // TODO: Aquí enviarías los datos al backend
                Toast.makeText(
                    context,
                    "Publicando: $titulo con ${imagenesSeleccionadas.size} imagen(es)",
                    Toast.LENGTH_SHORT
                ).show()

                // Ejemplo de lo que harías:
                /*
                val imagenesPaths = imagenesSeleccionadas.map { it.toString() }

                viewModel.crearPublicacion(
                    titulo = titulo,
                    descripcion = contenido,
                    imagenes = imagenesPaths
                ).observe(viewLifecycleOwner) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(context, "Publicación creada", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(context, "Error al publicar", Toast.LENGTH_SHORT).show()
                    }
                }
                */

                // Por ahora solo navegamos de vuelta
                findNavController().navigateUp()
            }
        }
    }
}