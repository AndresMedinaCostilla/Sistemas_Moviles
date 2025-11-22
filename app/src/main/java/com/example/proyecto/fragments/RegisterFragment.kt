package com.example.proyecto.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.proyecto.R
import com.example.proyecto.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class RegisterFragment : Fragment() {

    // Vistas del formulario
    private lateinit var imgPerfil: ImageView
    private lateinit var etNombre: EditText
    private lateinit var etApellidoPaterno: EditText
    private lateinit var etApellidoMaterno: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etUsuario: EditText
    private lateinit var btnRegistrarse: Button
    private lateinit var txtIniciarSesion: TextView

    // URI de la imagen seleccionada
    private var selectedImageUri: Uri? = null

    // Launcher para seleccionar imagen
    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // Mostrar la imagen seleccionada con Coil
            imgPerfil.load(uri) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.mipmap.user_foreground)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        imgPerfil = view.findViewById(R.id.imgPerfil)
        etNombre = view.findViewById(R.id.txtNombre)
        etApellidoPaterno = view.findViewById(R.id.txtapellidoPaterno)
        etApellidoMaterno = view.findViewById(R.id.txtapellidoMaterno)
        etCorreo = view.findViewById(R.id.txtCorreo)
        etContrasena = view.findViewById(R.id.txtContrasena)
        etTelefono = view.findViewById(R.id.txtTelefono)
        etUsuario = view.findViewById(R.id.txtUsuario)
        btnRegistrarse = view.findViewById(R.id.btnRegistrarse)
        txtIniciarSesion = view.findViewById(R.id.txtIniciarSesion)

        // Click en imagen de perfil para seleccionar foto
        imgPerfil.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Botón de registro
        btnRegistrarse.setOnClickListener {
            registrarUsuario()
        }

        // Link para ir al login
        txtIniciarSesion.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun registrarUsuario() {
        // Obtener valores de los campos
        val nombre = etNombre.text.toString().trim()
        val apellidoPaterno = etApellidoPaterno.text.toString().trim()
        val apellidoMaterno = etApellidoMaterno.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val telefono = etTelefono.text.toString().trim()
        val usuario = etUsuario.text.toString().trim()

        // Validaciones básicas
        if (!validarCampos(nombre, apellidoPaterno, correo, contrasena, usuario)) {
            return
        }

        // Deshabilitar botón mientras se procesa
        btnRegistrarse.isEnabled = false
        btnRegistrarse.text = "Registrando..."

        // Llamada a la API
        lifecycleScope.launch {
            try {
                // Preparar los datos como RequestBody
                val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
                val apellidoPaternoBody = apellidoPaterno.toRequestBody("text/plain".toMediaTypeOrNull())
                val apellidoMaternoBody = if (apellidoMaterno.isNotEmpty()) {
                    apellidoMaterno.toRequestBody("text/plain".toMediaTypeOrNull())
                } else null
                val usuarioBody = usuario.toRequestBody("text/plain".toMediaTypeOrNull())
                val correoBody = correo.toRequestBody("text/plain".toMediaTypeOrNull())
                val contrasenaBody = contrasena.toRequestBody("text/plain".toMediaTypeOrNull())
                val telefonoBody = if (telefono.isNotEmpty()) {
                    telefono.toRequestBody("text/plain".toMediaTypeOrNull())
                } else null

                // Preparar la imagen (si existe)
                val fotoPart = selectedImageUri?.let { uri ->
                    val file = uriToFile(uri)

                    // Obtener el tipo MIME real
                    val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"

                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("foto_perfil", file.name, requestFile)
                }

                // Hacer la petición
                val response = RetrofitClient.apiService.registro(
                    nombre = nombreBody,
                    apellidoPaterno = apellidoPaternoBody,
                    apellidoMaterno = apellidoMaternoBody,
                    usuario = usuarioBody,
                    correoElectronico = correoBody,
                    contrasena = contrasenaBody,
                    telefono = telefonoBody,
                    foto_perfil = fotoPart
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        requireContext(),
                        "¡Registro exitoso! Ahora puedes iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navegar al login
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)

                } else {
                    val errorMsg = response.body()?.message ?: "Error al registrar usuario"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                btnRegistrarse.isEnabled = true
                btnRegistrarse.text = "Registrarte"
            }
        }
    }

    // Convertir URI a File con tipo MIME correcto
    private fun uriToFile(uri: Uri): File {
        val contentResolver = requireContext().contentResolver

        // Obtener el tipo MIME real de la imagen
        val mimeType = contentResolver.getType(uri)

        // Determinar la extensión según el tipo MIME
        val extension = when (mimeType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "image/webp" -> ".webp"
            else -> ".jpg" // Por defecto
        }

        val tempFile = File(
            requireContext().cacheDir,
            "temp_image_${System.currentTimeMillis()}${extension}"
        )

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun validarCampos(
        nombre: String,
        apellidoPaterno: String,
        correo: String,
        contrasena: String,
        usuario: String
    ): Boolean {
        // Validar campos obligatorios
        if (nombre.isEmpty() || apellidoPaterno.isEmpty() ||
            correo.isEmpty() || contrasena.isEmpty() || usuario.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Completa todos los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Validar formato de email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(
                requireContext(),
                "El formato del correo electrónico no es válido",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Validar contraseña (mínimo 10 caracteres, 1 mayúscula, 1 minúscula, 1 número)
        if (contrasena.length < 10) {
            Toast.makeText(
                requireContext(),
                "La contraseña debe tener mínimo 10 caracteres",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (!contrasena.any { it.isUpperCase() }) {
            Toast.makeText(
                requireContext(),
                "La contraseña debe tener al menos una mayúscula",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (!contrasena.any { it.isLowerCase() }) {
            Toast.makeText(
                requireContext(),
                "La contraseña debe tener al menos una minúscula",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (!contrasena.any { it.isDigit() }) {
            Toast.makeText(
                requireContext(),
                "La contraseña debe tener al menos un número",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }
}