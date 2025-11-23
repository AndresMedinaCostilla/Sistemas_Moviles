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
import androidx.activity.OnBackPressedCallback
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
    private lateinit var btnGuardarBorrador: Button
    private lateinit var etTitulo: EditText
    private lateinit var etContenido: EditText
    private lateinit var tvContadorCaracteres: TextView
    private lateinit var tvContadorImagenes: TextView
    private lateinit var layoutImagenes: CardView
    private lateinit var recyclerImagenes: RecyclerView

    private lateinit var imagenesAdapter: ImagenesAdapter
    private val imagenesSeleccionadas = mutableListOf<Uri>()
    private var fotoUri: Uri? = null

    companion object {
        private const val KEY_IMAGENES = "imagenes_seleccionadas"
        private const val KEY_TITULO = "titulo"
        private const val KEY_CONTENIDO = "contenido"
        private const val KEY_FOTO_URI = "foto_uri"
    }

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                if (imagenesSeleccionadas.size < 3) {
                    imagenesSeleccionadas.add(uri)
                    imagenesAdapter.agregarImagen(uri)
                }
            }
            actualizarVistaImagenes()
        }
    }

    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && fotoUri != null) {
            if (imagenesSeleccionadas.size < 3) {
                imagenesSeleccionadas.add(fotoUri!!)
                imagenesAdapter.agregarImagen(fotoUri!!)
                actualizarVistaImagenes()
            }
        }
    }

    private val permisosCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamara()
        else Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    private val permisosGaleriaLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirGaleria()
        else Toast.makeText(context, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
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
        configurarBackPressed()

        // Restaurar estado si existe
        savedInstanceState?.let { restaurarEstado(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar imágenes
        val uriStrings = ArrayList(imagenesSeleccionadas.map { it.toString() })
        outState.putStringArrayList(KEY_IMAGENES, uriStrings)
        // Guardar texto
        outState.putString(KEY_TITULO, etTitulo.text.toString())
        outState.putString(KEY_CONTENIDO, etContenido.text.toString())
        // Guardar URI de foto temporal
        fotoUri?.let { outState.putString(KEY_FOTO_URI, it.toString()) }
    }

    private fun restaurarEstado(savedInstanceState: Bundle) {
        // Restaurar imágenes
        savedInstanceState.getStringArrayList(KEY_IMAGENES)?.let { uriStrings ->
            imagenesSeleccionadas.clear()
            for (uriString in uriStrings) {
                val uri = Uri.parse(uriString)
                imagenesSeleccionadas.add(uri)
                imagenesAdapter.agregarImagen(uri)
            }
            actualizarVistaImagenes()
        }
        // Restaurar texto
        savedInstanceState.getString(KEY_TITULO)?.let { etTitulo.setText(it) }
        savedInstanceState.getString(KEY_CONTENIDO)?.let { etContenido.setText(it) }
        // Restaurar URI de foto
        savedInstanceState.getString(KEY_FOTO_URI)?.let { fotoUri = Uri.parse(it) }
    }

    private fun inicializarVistas(view: View) {
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnGaleria = view.findViewById(R.id.btnGaleria)
        btnPublicar = view.findViewById(R.id.btnPublicar)
        btnGuardarBorrador = view.findViewById(R.id.btnGuardarBorrador)
        etTitulo = view.findViewById(R.id.etTitulo)
        etContenido = view.findViewById(R.id.etContenido)
        tvContadorCaracteres = view.findViewById(R.id.tvContadorCaracteres)
        tvContadorImagenes = view.findViewById(R.id.tvContadorImagenes)
        layoutImagenes = view.findViewById(R.id.layoutImagenes)
        recyclerImagenes = view.findViewById(R.id.recyclerImagenes)
    }

    private fun configurarRecyclerView() {
        imagenesAdapter = ImagenesAdapter(mutableListOf()) { position ->
            if (position in imagenesSeleccionadas.indices) {
                imagenesSeleccionadas.removeAt(position)
            }
            imagenesAdapter.eliminarImagen(position)
            actualizarVistaImagenes()
        }
        recyclerImagenes.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerImagenes.adapter = imagenesAdapter
    }

    private fun configurarListeners() {
        btnCancelar.setOnClickListener { mostrarDialogoSalir() }

        btnGaleria.setOnClickListener {
            if (imagenesSeleccionadas.size < 3) mostrarDialogoOpciones()
            else Toast.makeText(context, "Máximo 3 imágenes permitidas", Toast.LENGTH_SHORT).show()
        }

        btnPublicar.setOnClickListener { publicarContenido() }
        btnGuardarBorrador.setOnClickListener { guardarBorrador() }
    }

    private fun configurarBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mostrarDialogoSalir()
                }
            }
        )
    }

    private fun tieneContenido(): Boolean {
        return etTitulo.text.toString().trim().isNotEmpty() ||
                etContenido.text.toString().trim().isNotEmpty() ||
                imagenesSeleccionadas.isNotEmpty()
    }

    private fun mostrarDialogoSalir() {
        if (tieneContenido()) {
            AlertDialog.Builder(requireContext())
                .setTitle("¿Guardar borrador?")
                .setMessage("Tienes cambios sin guardar. ¿Deseas guardar esta publicación como borrador?")
                .setPositiveButton("Guardar borrador") { _, _ ->
                    guardarBorrador()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Descartar") { _, _ ->
                    findNavController().navigateUp()
                }
                .setNeutralButton("Cancelar", null)
                .show()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun configurarContadorCaracteres() {
        etContenido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvContadorCaracteres.text = "${s?.length ?: 0}/500"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun mostrarDialogoOpciones() {
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar imagen")
            .setItems(arrayOf("Tomar foto", "Elegir de galería")) { _, which ->
                when (which) {
                    0 -> verificarYAbrirCamara()
                    1 -> verificarYAbrirGaleria()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verificarYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            permisosCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun verificarYAbrirGaleria() {
        val permiso = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permiso) == PackageManager.PERMISSION_GRANTED) {
            abrirGaleria()
        } else {
            permisosGaleriaLauncher.launch(permiso)
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
        if (3 - imagenesSeleccionadas.size > 0) {
            galeriaLauncher.launch("image/*")
        }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun actualizarVistaImagenes() {
        layoutImagenes.visibility = if (imagenesSeleccionadas.isEmpty()) View.GONE else View.VISIBLE
        tvContadorImagenes.text = "${imagenesSeleccionadas.size}/3 imágenes"
    }

    private fun guardarBorrador() {
        val titulo = etTitulo.text.toString().trim()
        val contenido = etContenido.text.toString().trim()

        if (titulo.isEmpty() && contenido.isEmpty() && imagenesSeleccionadas.isEmpty()) {
            Toast.makeText(context, "No hay contenido para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Guardar en base de datos local o SharedPreferences
        Toast.makeText(context, "Borrador guardado", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun publicarContenido() {
        val titulo = etTitulo.text.toString().trim()
        val contenido = etContenido.text.toString().trim()

        when {
            titulo.isEmpty() -> Toast.makeText(context, "Agrega un título", Toast.LENGTH_SHORT).show()
            contenido.isEmpty() -> Toast.makeText(context, "Agrega contenido", Toast.LENGTH_SHORT).show()
            else -> {
                // TODO: Enviar al backend
                Toast.makeText(context, "Publicando: $titulo con ${imagenesSeleccionadas.size} imagen(es)", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
}