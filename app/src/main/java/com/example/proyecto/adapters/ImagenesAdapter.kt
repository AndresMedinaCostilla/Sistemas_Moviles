package com.example.proyecto.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R

class ImagenesAdapter(
    private val imagenes: MutableList<Uri>,
    private val onEliminarClick: (Int) -> Unit
) : RecyclerView.Adapter<ImagenesAdapter.ImagenViewHolder>() {

    inner class ImagenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPreview: ImageView = itemView.findViewById(R.id.imgPreview)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)

        fun bind(uri: Uri, position: Int) {
            imgPreview.setImageURI(uri)
            btnEliminar.setOnClickListener {
                onEliminarClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_preview, parent, false)
        return ImagenViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagenViewHolder, position: Int) {
        holder.bind(imagenes[position], position)
    }

    override fun getItemCount(): Int = imagenes.size

    fun agregarImagen(uri: Uri) {
        if (imagenes.size < 3) {
            imagenes.add(uri)
            notifyItemInserted(imagenes.size - 1)
        }
    }

    fun eliminarImagen(position: Int) {
        if (position in imagenes.indices) {
            imagenes.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imagenes.size)
        }
    }

    fun getImagenes(): List<Uri> = imagenes
}