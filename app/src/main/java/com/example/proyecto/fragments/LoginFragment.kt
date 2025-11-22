package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.proyecto.R
import com.example.proyecto.models.requests.LoginRequest
import com.example.proyecto.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var txtRegistro: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas con los IDs correctos del XML
        etCorreo = view.findViewById(R.id.txtCorreo)
        etContrasena = view.findViewById(R.id.txtContrasena)
        btnIniciarSesion = view.findViewById(R.id.btnIniciarSesion)
        txtRegistro = view.findViewById(R.id.txtRegistro)

        // Botón de Login
        btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        // Ir a registro
        txtRegistro.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun iniciarSesion() {
        val usuario = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString().trim()

        // Validaciones básicas
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón mientras se procesa
        btnIniciarSesion.isEnabled = false
        btnIniciarSesion.text = "Iniciando..."

        // Llamada a la API con Coroutines
        lifecycleScope.launch {
            try {
                val request = LoginRequest(usuario, contrasena)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val userData = response.body()?.data

                    Toast.makeText(
                        requireContext(),
                        "¡Bienvenido ${userData?.nombre}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Aquí podrías guardar los datos del usuario (SharedPreferences)
                    // y navegar al Home
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)

                } else {
                    val errorMsg = response.body()?.message ?: "La contraseña o el correo no coinciden"
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
                btnIniciarSesion.isEnabled = true
                btnIniciarSesion.text = "Iniciar sesión"
            }
        }
    }
}