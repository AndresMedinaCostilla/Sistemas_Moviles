package com.example.proyecto

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val txtRegistro = findViewById<TextView>(R.id.txtRegistro)
        txtRegistro.setOnClickListener {
            irARegistro()
        }
    }

    // Esta función DEBE estar DENTRO de la clase MainActivity
    private fun irARegistro() {
        try {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Muestra un mensaje de error
            android.widget.Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}