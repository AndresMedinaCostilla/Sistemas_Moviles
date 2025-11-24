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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.ImagenesAdapter
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.repository.PublicacionRepository
import com.example.proyecto.utils.SessionManager
import com.example.proyecto.utils.NetworkUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AgregarPublicacionFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var publicacionRepository: PublicacionRepository
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

        sessionManager = SessionManager(requireContext())
        publicacionRepository = PublicacionRepository(requireContext())

        inicializarVistas(view)
        configurarRecyclerView()
        configurarListeners()
        configurarContadorCaracteres()
        configurarBackPressed()

        savedInstanceState?.let { restaurarEstado(it) }

        // Verificar si hay publicaciones pendientes de sincronizar
        verificarPublicacionesPendientes()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val uriStrings = ArrayList(imagenesSeleccionadas.map { it.toString() })
        outState.putStringArrayList(KEY_IMAGENES, uriStrings)
        outState.putString(KEY_TITULO, etTitulo.text.toString())
        outState.putString(KEY_CONTENIDO, etContenido.text.toString())
        fotoUri?.let { outState.putString(KEY_FOTO_URI, it.toString()) }
    }

    private fun restaurarEstado(savedInstanceState: Bundle) {
        savedInstanceState.getStringArrayList(KEY_IMAGENES)?.let { uriStrings ->
            imagenesSeleccionadas.clear()
            for (uriString in uriStrings) {
                val uri = Uri.parse(uriString)
                imagenesSeleccionadas.add(uri)
                imagenesAdapter.agregarImagen(uri)
            }
            actualizarVistaImagenes()
        }
        savedInstanceState.getString(KEY_TITULO)?.let { etTitulo.setText(it) }
        savedInstanceState.getString(KEY_CONTENIDO)?.let { etContenido.setText(it) }
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

    // ==================== VERIFICAR PUBLICACIONES PENDIENTES ====================

    private fun verificarPublicacionesPendientes() {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val pendientes = publicacionRepository.contarPendientes(userId)

                if (pendientes > 0 && NetworkUtils.isInternetAvailable(requireContext())) {
                    mostrarDialogoSincronizar(pendientes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mostrarDialogoSincronizar(cantidad: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Publicaciones pendientes")
            .setMessage("Tienes $cantidad publicación(es) guardada(s) sin conexión. ¿Deseas subirlas ahora?")
            .setPositiveButton("Subir ahora") { _, _ ->
                sincronizarPublicaciones()
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    private fun sincronizarPublicaciones() {
        lifecycleScope.launch {
            try {
                Toast.makeText(context, "Sincronizando publicaciones...", Toast.LENGTH_SHORT).show()

                val userId = sessionManager.getUserId()
                val resultado = publicacionRepository.sincronizarPublicacionesPendientes(userId)

                Toast.makeText(context, resultado.mensaje, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error al sincronizar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==================== GUARDAR BORRADOR ====================

    private fun guardarBorrador() {
        val titulo = etTitulo.text.toString().trim()
        val contenido = etContenido.text.toString().trim()

        if (titulo.isEmpty() && contenido.isEmpty() && imagenesSeleccionadas.isEmpty()) {
            Toast.makeText(context, "No hay contenido para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarBorrador.isEnabled = false
        btnGuardarBorrador.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()

                publicacionRepository.guardarPublicacionLocal(
                    idUsuario = userId,
                    titulo = titulo.ifEmpty { "Sin título" },
                    contenido = contenido.ifEmpty { "Sin contenido" },
                    imagenesUris = imagenesSeleccionadas,
                    esBorrador = true
                )

                Toast.makeText(context, "✅ Borrador guardado localmente", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error al guardar borrador: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                btnGuardarBorrador.isEnabled = true
                btnGuardarBorrador.text = "Guardar borrador"
            }
        }
    }

    // ==================== PUBLICAR CONTENIDO ====================

    private fun publicarContenido() {
        val titulo = etTitulo.text.toString().trim()
        val contenido = etContenido.text.toString().trim()

        when {
            titulo.isEmpty() -> {
                Toast.makeText(context, "Agrega un título", Toast.LENGTH_SHORT).show()
                return
            }
            contenido.isEmpty() -> {
                Toast.makeText(context, "Agrega contenido", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Verificar conexión a internet
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            mostrarDialogoGuardarSinConexion(titulo, contenido)
            return
        }

        // Publicar directamente si hay internet
        publicarEnServidor(titulo, contenido)
    }

    private fun mostrarDialogoGuardarSinConexion(titulo: String, contenido: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Sin conexión a internet")
            .setMessage("No hay conexión disponible. La publicación se guardará localmente y se subirá automáticamente cuando tengas internet.")
            .setPositiveButton("Guardar") { _, _ ->
                guardarPublicacionLocal(titulo, contenido, esBorrador = false)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarPublicacionLocal(titulo: String, contenido: String, esBorrador: Boolean) {
        btnPublicar.isEnabled = false
        btnPublicar.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()

                publicacionRepository.guardarPublicacionLocal(
                    idUsuario = userId,
                    titulo = titulo,
                    contenido = contenido,
                    imagenesUris = imagenesSeleccionadas,
                    esBorrador = esBorrador
                )

                val mensaje = if (esBorrador) {
                    "📝 Borrador guardado"
                } else {
                    "💾 Publicación guardada. Se subirá cuando tengas internet"
                }

                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                btnPublicar.isEnabled = true
                btnPublicar.text = "PUBLICAR"
            }
        }
    }

    private fun publicarEnServidor(titulo: String, contenido: String) {
        btnPublicar.isEnabled = false
        btnPublicar.text = "Publicando..."

        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()

                val idUsuarioBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val tituloBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
                val descripcionBody = contenido.toRequestBody("text/plain".toMediaTypeOrNull())

                val imagenesParts = mutableListOf<MultipartBody.Part>()
                for (uri in imagenesSeleccionadas) {
                    val file = uriToFile(uri)
                    if (file != null) {
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("imagenes", file.name, requestFile)
                        imagenesParts.add(part)
                    }
                }

                val response = RetrofitClient.publicacionesApi.crearPublicacion(
                    idUsuario = idUsuarioBody,
                    titulo = tituloBody,
                    descripcion = descripcionBody,
                    imagenes = imagenesParts
                )

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        Toast.makeText(
                            context,
                            "✅ Publicación creada exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(
                            context,
                            responseBody.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Error al crear publicación: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                btnPublicar.isEnabled = true
                btnPublicar.text = "PUBLICAR"
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload", ".jpg", requireContext().cacheDir)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}