package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyecto.R

class RegisterFragment : Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.activity_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Encontrar el TextView por su ID (txtIniciarSesion)
        val registroTextView = view.findViewById<TextView>(R.id.txtIniciarSesion)

        // 2. Asignar el listener de clic para ir a RegistroFragment
        registroTextView.setOnClickListener {
            // Usa el ID de la acción definida en nav_graph.xml
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }
}