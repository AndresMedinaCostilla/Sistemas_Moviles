package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyecto.R
import androidx.navigation.fragment.findNavController
import android.widget.TextView

class LoginFragment : Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.activity_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Encontrar el TextView por su ID
        val registroTextView = view.findViewById<TextView>(R.id.txtRegistro)

        // 2. Asignar el listener de clic
        registroTextView.setOnClickListener {
            // 3. Navegar usando el ID de la acción definida en nav_graph.xml
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}