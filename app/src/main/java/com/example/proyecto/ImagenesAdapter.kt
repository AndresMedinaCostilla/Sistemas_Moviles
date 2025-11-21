package com.example.proyecto

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImagenesAdapter(
    private val imagenes: List<Uri>,
    private val onEliminarClick: (Int) -> Unit
) : RecyclerView.Adapter<ImagenesAdapter.ImagenViewHolder>() {

    inner class ImagenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.ivImagenSeleccionada)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarImagen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_seleccionada, parent, false)
        return ImagenViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagenViewHolder, position: Int) {
        val imagenUri = imagenes[position]

        // Cargar imagen (por ahora sin Glide)
        holder.imagen.setImageURI(imagenUri)

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(position)
        }
    }

    override fun getItemCount(): Int = imagenes.size
}