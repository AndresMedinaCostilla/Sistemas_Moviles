package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.proyecto.R
import com.example.proyecto.models.Publicacion

class PublicacionesAdapter(
    var publicaciones: List<Publicacion>,  // ✅ Cambiado a 'var' sin 'private'
    private val onLikeClick: (Publicacion) -> Unit,
    private val onDislikeClick: (Publicacion) -> Unit,
    private val onCommentClick: (Publicacion) -> Unit,
    private val onFavoriteClick: (Publicacion) -> Unit,
    private val onPublicacionClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.PublicacionViewHolder>() {

    inner class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewPagerImagenes: ViewPager2 = itemView.findViewById(R.id.viewPagerImagenes)
        val layoutIndicadores: LinearLayout = itemView.findViewById(R.id.layoutIndicadores)
        val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        val txtDescripcion: TextView = itemView.findViewById(R.id.txtDescripcion)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val txtLikes: TextView = itemView.findViewById(R.id.txtLikes)
        val btnDislike: ImageView = itemView.findViewById(R.id.btnDislike)
        val txtDislikes: TextView = itemView.findViewById(R.id.txtDislikes)
        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        val txtComments: TextView = itemView.findViewById(R.id.txtComments)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)

        fun bind(publicacion: Publicacion) {
            // Configurar textos
            txtTitulo.text = publicacion.titulo
            txtDescripcion.text = publicacion.descripcion
            txtFecha.text = publicacion.fecha
            txtLikes.text = publicacion.likes.toString()
            txtDislikes.text = publicacion.dislikes.toString()
            txtComments.text = publicacion.comentarios.toString()

            // ✅ Configurar apariencia del botón Like
            if (publicacion.usuarioLike) {
                btnLike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.like_activo)
                )
            } else {
                btnLike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
            }

            // ✅ Configurar apariencia del botón Dislike
            if (publicacion.usuarioDislike) {
                btnDislike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.dislike_activo)
                )
            } else {
                btnDislike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
            }

            // ✅ Configurar apariencia del botón Favorito
            if (publicacion.usuarioFavorito) {
                btnFavorite.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.favorito_activo)
                )
            } else {
                btnFavorite.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
            }

            // Configurar ViewPager para imágenes
            if (publicacion.imagenesUrl.isNotEmpty()) {
                val imagenesAdapter = ImagenesPublicacionUrlAdapter(publicacion.imagenesUrl)
                viewPagerImagenes.adapter = imagenesAdapter

                // Configurar indicadores si hay más de una imagen
                if (publicacion.imagenesUrl.size > 1) {
                    setupIndicadores(publicacion.imagenesUrl.size, 0)
                    viewPagerImagenes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            setupIndicadores(publicacion.imagenesUrl.size, position)
                        }
                    })
                } else {
                    layoutIndicadores.removeAllViews()
                }
            }

            // Listeners
            btnLike.setOnClickListener { onLikeClick(publicacion) }
            btnDislike.setOnClickListener { onDislikeClick(publicacion) }
            btnComment.setOnClickListener { onCommentClick(publicacion) }
            btnFavorite.setOnClickListener { onFavoriteClick(publicacion) }
            itemView.setOnClickListener { onPublicacionClick(publicacion) }
        }

        private fun setupIndicadores(total: Int, seleccionado: Int) {
            layoutIndicadores.removeAllViews()

            for (i in 0 until total) {
                val indicador = View(itemView.context)
                val params = LinearLayout.LayoutParams(16, 16)
                params.setMargins(4, 0, 4, 0)
                indicador.layoutParams = params

                if (i == seleccionado) {
                    indicador.setBackgroundResource(R.drawable.indicator_active)
                } else {
                    indicador.setBackgroundResource(R.drawable.indicator_inactive)
                }

                layoutIndicadores.addView(indicador)
            }
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

    fun updatePublicaciones(nuevasPublicaciones: List<Publicacion>) {
        publicaciones = nuevasPublicaciones
        notifyDataSetChanged()
    }
}