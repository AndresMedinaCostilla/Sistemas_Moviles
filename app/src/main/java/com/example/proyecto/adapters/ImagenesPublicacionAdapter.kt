package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R

class ImagenesPublicacionAdapter(
    private val imagenes: List<Int>
) : RecyclerView.Adapter<ImagenesPublicacionAdapter.ImagenViewHolder>() {

    inner class ImagenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPublicacion: ImageView = itemView.findViewById(R.id.imgPublicacion)

        fun bind(drawableRes: Int) {
            imgPublicacion.setImageResource(drawableRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_publicacion, parent, false)
        return ImagenViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagenViewHolder, position: Int) {
        holder.bind(imagenes[position])
    }

    override fun getItemCount(): Int = imagenes.size
}