package com.example.proyecto.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.network.ActualizarPerfilRequest
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EditarPerfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    // Vistas
    private lateinit var imgPerfil: ImageView
    private lateinit var txtNombre: EditText
    private lateinit var txtApellidoPaterno: EditText
    private lateinit var txtApellidoMaterno: EditText
    private lateinit var txtContrasena: EditText
    private lateinit var txtTelefono: EditText
    private lateinit var btnGuardarCambios: Button
    private lateinit var txtRegresar: TextView

    // URI de la imagen seleccionada
    private var imagenSeleccionadaUri: Uri? = null

    // Launcher para seleccionar imagen de galería
    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imagenSeleccionadaUri = uri
                // Mostrar la imagen seleccionada
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(imgPerfil)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_editar_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        // Verificar sesión activa
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(context, "No hay sesión activa", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_editarPerfilFragment_to_perfilFragment)
            return
        }

        // Inicializar vistas
        inicializarVistas(view)

        // Cargar datos actuales del usuario
        cargarDatosActuales()

        // Configurar listeners
        setupListeners()
    }

    private fun inicializarVistas(view: View) {
        imgPerfil = view.findViewById(R.id.imgPerfil)
        txtNombre = view.findViewById(R.id.txtNombre)
        txtApellidoPaterno = view.findViewById(R.id.txtapellidoPaterno)
        txtApellidoMaterno = view.findViewById(R.id.txtapellidoMaterno)
        txtContrasena = view.findViewById(R.id.txtContrasena)
        txtTelefono = view.findViewById(R.id.txtTelefono)
        btnGuardarCambios = view.findViewById(R.id.btnRegistrarse)
        txtRegresar = view.findViewById(R.id.txtRegresar) ?: buscarTextViewPorTexto(view, "Regresar")
    }

    private fun buscarTextViewPorTexto(view: View, texto: String): TextView {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is TextView && child.text.toString().contains(texto, ignoreCase = true)) {
                    return child
                }
                if (child is ViewGroup) {
                    val found = buscarTextViewPorTexto(child, texto)
                    if (found.text.isNotEmpty()) return found
                }
            }
        }
        return TextView(requireContext())
    }

    private fun cargarDatosActuales() {
        val userData = sessionManager.getUserData()

        if (userData != null) {
            // Cargar datos en los campos
            txtNombre.setText(userData.nombre)
            txtApellidoPaterno.setText(userData.apellidoPaterno ?: "")
            txtApellidoMaterno.setText(userData.apellidoMaterno ?: "")
            txtTelefono.setText(userData.telefono ?: "")

            // La contraseña se deja vacía por seguridad
            txtContrasena.setText("")
            txtContrasena.hint = "Dejar vacío para no cambiar"

            // Cargar foto de perfil actual
            val fotoPerfil = userData.fotoPerfil
            if (!fotoPerfil.isNullOrEmpty()) {
                val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                val fullImageUrl = "$baseUrl$fotoPerfil"

                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(imgPerfil)
            } else {
                imgPerfil.setImageResource(R.drawable.user)
            }

            println("📝 Datos cargados para edición: ${userData.nombre}")
        } else {
            Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        // Click en la imagen para cambiar foto
        imgPerfil.setOnClickListener {
            abrirGaleria()
        }

        // Botón guardar cambios
        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        // Botón regresar
        txtRegresar.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        seleccionarImagenLauncher.launch(intent)
    }

    private fun guardarCambios() {
        // Validar campos
        val nombre = txtNombre.text.toString().trim()
        val apellidoPaterno = txtApellidoPaterno.text.toString().trim()
        val apellidoMaterno = txtApellidoMaterno.text.toString().trim()
        val telefono = txtTelefono.text.toString().trim()
        val contrasena = txtContrasena.text.toString().trim()

        if (nombre.isEmpty()) {
            txtNombre.error = "El nombre es obligatorio"
            return
        }

        if (apellidoPaterno.isEmpty()) {
            txtApellidoPaterno.error = "El apellido paterno es obligatorio"
            return
        }

        // Validar contraseña si se ingresó una nueva
        if (contrasena.isNotEmpty()) {
            if (contrasena.length < 10) {
                txtContrasena.error = "La contraseña debe tener mínimo 10 caracteres"
                return
            }
            if (!contrasena.matches(Regex(".*[A-Z].*"))) {
                txtContrasena.error = "Debe contener al menos una mayúscula"
                return
            }
            if (!contrasena.matches(Regex(".*[a-z].*"))) {
                txtContrasena.error = "Debe contener al menos una minúscula"
                return
            }
            if (!contrasena.matches(Regex(".*\\d.*"))) {
                txtContrasena.error = "Debe contener al menos un número"
                return
            }
        }

        // Mostrar progreso
        btnGuardarCambios.isEnabled = false
        btnGuardarCambios.text = "Guardando..."

        // Actualizar perfil
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()

                // 1. Actualizar datos del perfil
                val request = ActualizarPerfilRequest(
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    telefono = telefono.ifEmpty { null },
                    contrasena = contrasena.ifEmpty { null }
                )

                val response = RetrofitClient.perfilApi.actualizarPerfil(userId, request)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        val usuarioActualizado = responseBody.usuario

                        // 2. Si hay una nueva imagen, subirla
                        if (imagenSeleccionadaUri != null) {
                            subirFotoPerfil(userId)
                        } else {
                            // Actualizar sesión con los nuevos datos
                            sessionManager.saveUserSession(usuarioActualizado)

                            Toast.makeText(
                                context,
                                "Perfil actualizado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Volver al perfil
                            findNavController().navigateUp()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            responseBody.message,
                            Toast.LENGTH_LONG
                        ).show()
                        btnGuardarCambios.isEnabled = true
                        btnGuardarCambios.text = "Guardar cambios"
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Error al actualizar perfil: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnGuardarCambios.isEnabled = true
                    btnGuardarCambios.text = "Guardar cambios"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardarCambios.isEnabled = true
                btnGuardarCambios.text = "Guardar cambios"
                e.printStackTrace()
            }
        }
    }

    private suspend fun subirFotoPerfil(userId: Int) {
        try {
            val uri = imagenSeleccionadaUri ?: return

            // Convertir URI a File
            val file = uriToFile(uri)

            if (file != null) {
                // Crear RequestBody y MultipartBody.Part
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("foto", file.name, requestFile)

                // Subir foto
                val response = RetrofitClient.perfilApi.actualizarFotoPerfil(userId, body)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    if (responseBody.success) {
                        val nuevaFotoUrl = responseBody.fotoPerfil

                        // Actualizar sesión con la nueva foto
                        val userData = sessionManager.getUserData()
                        if (userData != null) {
                            val usuarioActualizado = userData.copy(fotoPerfil = nuevaFotoUrl)
                            sessionManager.saveUserSession(usuarioActualizado)
                        }

                        Toast.makeText(
                            context,
                            "Perfil y foto actualizados exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Volver al perfil
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(
                            context,
                            responseBody.message,
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigateUp()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Perfil actualizado, pero hubo un error al subir la foto",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al subir foto: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
            findNavController().navigateUp()
        } finally {
            btnGuardarCambios.isEnabled = true
            btnGuardarCambios.text = "Guardar cambios"
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