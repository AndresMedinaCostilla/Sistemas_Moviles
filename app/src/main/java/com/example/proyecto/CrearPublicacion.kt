package com.example.proyecto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R

class CrearPublicacionActivity : AppCompatActivity() {

    private lateinit var recyclerImagenes: RecyclerView
    private lateinit var layoutImagenes: LinearLayout
    private lateinit var btnGaleria: ImageButton
    private lateinit var btnPublicar: Button
    private lateinit var btnCancelar: ImageButton
    private lateinit var etTitulo: EditText
    private lateinit var etContenido: EditText
    private lateinit var tvContadorCaracteres: TextView

    private val imagenesSeleccionadas = mutableListOf<Uri>()

    companion object {
        private const val REQUEST_CODE_GALERIA = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_publicacion) // Asegúrate que este sea el nombre de tu XML

        setupViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun setupViews() {
        recyclerImagenes = findViewById(R.id.recyclerImagenes)
        layoutImagenes = findViewById(R.id.layoutImagenes)
        btnGaleria = findViewById(R.id.btnGaleria)
        btnPublicar = findViewById(R.id.btnPublicar)
        btnCancelar = findViewById(R.id.btnCancelar)
        etTitulo = findViewById(R.id.etTitulo)
        etContenido = findViewById(R.id.etContenido)
        tvContadorCaracteres = findViewById(R.id.tvContadorCaracteres)
    }

    private fun setupRecyclerView() {
        recyclerImagenes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = ImagenesAdapter(imagenesSeleccionadas, { position ->
            imagenesSeleccionadas.removeAt(position)
            actualizarVistaImagenes()
        })
        recyclerImagenes.adapter = adapter
    }

    private fun setupListeners() {
        btnGaleria.setOnClickListener {
            abrirGaleria()
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnPublicar.setOnClickListener {
            publicarContenido()
        }

        etContenido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvContadorCaracteres.text = "${s?.length ?: 0}/500"
            }
        })
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Seleccionar imágenes"), REQUEST_CODE_GALERIA)
    }

    private fun actualizarVistaImagenes() {
        if (imagenesSeleccionadas.isNotEmpty()) {
            layoutImagenes.visibility = View.VISIBLE
            recyclerImagenes.adapter?.notifyDataSetChanged()
        } else {
            layoutImagenes.visibility = View.GONE
        }
    }

    private fun publicarContenido() {
        val titulo = etTitulo.text.toString().trim()
        val contenido = etContenido.text.toString().trim()

        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (contenido.isEmpty()) {
            Toast.makeText(this, "El contenido es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Publicación creada", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GALERIA && resultCode == RESULT_OK) {
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    imagenesSeleccionadas.add(imageUri)
                }
            } else if (data?.data != null) {
                imagenesSeleccionadas.add(data.data!!)
            }
            actualizarVistaImagenes()
        }
    }
}