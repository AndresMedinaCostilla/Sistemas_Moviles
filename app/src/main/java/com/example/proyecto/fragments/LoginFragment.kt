package com.example.proyecto.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.proyecto.R
import com.example.proyecto.models.requests.LoginRequest
import com.example.proyecto.network.RetrofitClient
import com.example.proyecto.utils.SessionManager
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var txtRegistro: TextView

    // SessionManager para guardar la sesión
    private lateinit var sessionManager: SessionManager

    // Variables para el doble tap para salir
    private var backPressedOnce = false
    private val backPressHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar SessionManager
        sessionManager = SessionManager(requireContext())

        // Verificar si ya hay sesión activa
        if (sessionManager.isLoggedIn()) {
            // Si ya está logueado, ir directo al Home
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // Inicializar vistas con los IDs correctos del XML
        etCorreo = view.findViewById(R.id.txtCorreo)
        etContrasena = view.findViewById(R.id.txtContrasena)
        btnIniciarSesion = view.findViewById(R.id.btnIniciarSesion)
        txtRegistro = view.findViewById(R.id.txtRegistro)

        // Configurar el manejo del botón atrás
        configurarBackPressed()

        // Botón de Login
        btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        // Ir a registro
        txtRegistro.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun configurarBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPressedOnce) {
                        // Segunda vez presionado: salir de la app
                        requireActivity().finish()
                    } else {
                        // Primera vez presionado: mostrar mensaje
                        backPressedOnce = true
                        Toast.makeText(
                            requireContext(),
                            "Presiona atrás nuevamente para salir",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Resetear después de 2 segundos
                        backPressHandler.postDelayed({
                            backPressedOnce = false
                        }, 2000)
                    }
                }
            }
        )
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

                    if (userData != null) {
                        // 🔥 GUARDAR LA SESIÓN CON UsuarioData
                        sessionManager.saveUserSession(userData)

                        Toast.makeText(
                            requireContext(),
                            "¡Bienvenido ${userData.nombre}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navegar al Home
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error: No se recibieron datos del usuario",
                            Toast.LENGTH_LONG
                        ).show()
                    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar el handler para evitar memory leaks
        backPressHandler.removeCallbacksAndMessages(null)
    }
}