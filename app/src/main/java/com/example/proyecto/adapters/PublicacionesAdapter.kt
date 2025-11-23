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
    private var publicaciones: List<Publicacion>,
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
        val txtLikes: TextView = itemView.findViewById(R.id.txtLikes)
        val txtDislikes: TextView = itemView.findViewById(R.id.txtDislikes)
        val txtComments: TextView = itemView.findViewById(R.id.txtComments)
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val btnDislike: ImageView = itemView.findViewById(R.id.btnDislike)
        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)

        fun bind(publicacion: Publicacion) {
            txtTitulo.text = publicacion.titulo
            txtDescripcion.text = publicacion.descripcion
            txtFecha.text = publicacion.fecha
            txtLikes.text = publicacion.likes.toString()
            txtDislikes.text = publicacion.dislikes.toString()
            txtComments.text = publicacion.comentarios.toString()

            // Convertir las URLs de String a recursos drawable (para datos de ejemplo)
            val imagenesDrawable = publicacion.imagenesUrl.map { url ->
                when (url) {
                    "gato1" -> R.mipmap.gato_round
                    "gato2" -> R.drawable.like // TEMPORAL: Reemplaza con tus imágenes reales
                    "gato3" -> R.drawable.chat // TEMPORAL: Reemplaza con tus imágenes reales
                    "user" -> R.drawable.user
                    "home" -> R.drawable.home
                    "star" -> R.drawable.star
                    "dot" -> R.drawable.circle_background
                    "add" -> R.drawable.add
                    else -> R.mipmap.gato_round
                }
            }

            // Configurar ViewPager2 con las imágenes
            val imagenesAdapter = ImagenesPublicacionAdapter(imagenesDrawable)
            viewPagerImagenes.adapter = imagenesAdapter

            // Configurar indicadores de página (dots)
            setupIndicadores(imagenesDrawable.size)
            viewPagerImagenes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    actualizarIndicadores(position)
                }
            })

            // Listeners
            btnLike.setOnClickListener { onLikeClick(publicacion) }
            btnDislike.setOnClickListener { onDislikeClick(publicacion) }
            btnComment.setOnClickListener { onCommentClick(publicacion) }
            btnFavorite.setOnClickListener { onFavoriteClick(publicacion) }
            itemView.setOnClickListener { onPublicacionClick(publicacion) }
        }

        private fun setupIndicadores(cantidad: Int) {
            layoutIndicadores.removeAllViews()

            if (cantidad <= 1) {
                layoutIndicadores.visibility = View.GONE
                return
            }

            layoutIndicadores.visibility = View.VISIBLE
            val indicadores = arrayOfNulls<ImageView>(cantidad)
            val layoutParams = LinearLayout.LayoutParams(8.dpToPx(), 8.dpToPx()) // Tamaño del dot
            layoutParams.setMargins(4, 0, 4, 0)

            for (i in indicadores.indices) {
                indicadores[i] = ImageView(itemView.context)
                indicadores[i]?.setImageDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.indicator_dot  // <-- CAMBIAR AQUÍ
                    )
                )
                indicadores[i]?.layoutParams = layoutParams
                indicadores[i]?.setColorFilter(
                    ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                )
                layoutIndicadores.addView(indicadores[i])
            }

            if (indicadores.isNotEmpty()) {
                indicadores[0]?.setColorFilter(
                    ContextCompat.getColor(itemView.context, android.R.color.white)
                )
            }
        }

        // Función de extensión para convertir dp a pixels
        private fun Int.dpToPx(): Int {
            return (this * itemView.context.resources.displayMetrics.density).toInt()
        }

        private fun actualizarIndicadores(posicion: Int) {
            val childCount = layoutIndicadores.childCount
            for (i in 0 until childCount) {
                val imageView = layoutIndicadores.getChildAt(i) as ImageView
                if (i == posicion) {
                    imageView.setColorFilter(
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                    )
                } else {
                    imageView.setColorFilter(
                        ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                    )
                }
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