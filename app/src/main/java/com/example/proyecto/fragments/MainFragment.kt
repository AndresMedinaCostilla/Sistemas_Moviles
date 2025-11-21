package com.example.proyecto.fragments

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.proyecto.R

class MainFragment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Llama al layout que ahora contiene el NavHostFragment
        setContentView(R.layout.activity_main)
    }
}