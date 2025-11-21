package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView // Necesario para txtRegistro
import androidx.fragment.app.Fragment
import com.example.proyecto.R
import androidx.navigation.fragment.findNavController

class LoginFragment : Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_login, container, false)
    }

    // El código para la lógica y la navegación va aquí
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Encontrar el TextView por su ID (txtRegistro)
        val registroTextView = view.findViewById<TextView>(R.id.txtRegistro)

        val HomeTextView = view.findViewById<TextView>(R.id.btnIniciarSesion)

        // 2. Asignar el listener de clic para ir a RegistroFragment
        registroTextView.setOnClickListener {
            // Usa el ID de la acción definida en nav_graph.xml
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        HomeTextView.setOnClickListener {
            // Usa el ID de la acción definida en nav_graph.xml
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }

    }
}