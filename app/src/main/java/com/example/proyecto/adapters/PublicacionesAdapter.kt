package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.models.Publicacion

class PublicacionesAdapter(
    private var publicaciones: List<Publicacion>,
    private val onLikeClick: (Publicacion) -> Unit,
    private val onCommentClick: (Publicacion) -> Unit,
    private val onFavoriteClick: (Publicacion) -> Unit,
    private val onPublicacionClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.PublicacionViewHolder>() {

    inner class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPublicacion: ImageView = itemView.findViewById(R.id.imgPublicacion)
        val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        val txtDescripcion: TextView = itemView.findViewById(R.id.txtDescripcion)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val txtLikes: TextView = itemView.findViewById(R.id.txtLikes)
        val txtComments: TextView = itemView.findViewById(R.id.txtComments)
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)

        fun bind(publicacion: Publicacion) {
            txtTitulo.text = publicacion.titulo
            txtDescripcion.text = publicacion.descripcion
            txtFecha.text = publicacion.fecha
            txtLikes.text = publicacion.likes.toString()
            txtComments.text = publicacion.comentarios.toString()

            // TODO: Cargar imagen con Glide o Picasso cuando conectes a la BD
            // Glide.with(itemView.context).load(publicacion.imagenUrl).into(imgPublicacion)

            // Listeners
            btnLike.setOnClickListener { onLikeClick(publicacion) }
            btnComment.setOnClickListener { onCommentClick(publicacion) }
            btnFavorite.setOnClickListener { onFavoriteClick(publicacion) }
            itemView.setOnClickListener { onPublicacionClick(publicacion) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicacion, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionViewHolder, position: Int) {
        holder.bind(publicaciones[position])
    }

    override fun getItemCount(): Int = publicaciones.size

    // Método para actualizar la lista (cuando traigas datos de la BD)
    fun updatePublicaciones(nuevasPublicaciones: List<Publicacion>) {
        publicaciones = nuevasPublicaciones
        notifyDataSetChanged()
    }
}