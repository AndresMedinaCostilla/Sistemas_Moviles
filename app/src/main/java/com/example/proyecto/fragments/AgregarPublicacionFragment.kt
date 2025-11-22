package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Importa ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Importa NavController
import com.example.proyecto.R

class AgregarPublicacionFragment : Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_agregar_publicacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Encontrar el ImageButton de Cancelar
        val btnCancelar = view.findViewById<ImageButton>(R.id.btnCancelar)

        // 2. Asignar el listener de clic
        btnCancelar.setOnClickListener {
            // Este método simula la pulsación del botón 'Atrás' del sistema.
            // Lleva al usuario al Fragmento anterior en la pila de navegación.
            findNavController().navigateUp()
        }
    }
}