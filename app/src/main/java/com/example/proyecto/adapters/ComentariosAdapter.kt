package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.models.Comentario

class ComentariosAdapter(
    private var comentarios: List<Comentario>,
    private val onLikeClick: (Comentario) -> Unit
) : RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder>() {

    inner class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        val tvCommentText: TextView = itemView.findViewById(R.id.tvCommentText)
        val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        val btnToggleReplies: LinearLayout = itemView.findViewById(R.id.btnToggleReplies)
        val tvRepliesCount: TextView = itemView.findViewById(R.id.tvRepliesCount)
        val ivArrowReplies: ImageView = itemView.findViewById(R.id.ivArrowReplies)
        val layoutReplies: LinearLayout = itemView.findViewById(R.id.layoutReplies)
        val recyclerReplies: RecyclerView = itemView.findViewById(R.id.recyclerReplies)

        private var respuestasVisible = false

        fun bind(comentario: Comentario) {
            tvUserName.text = comentario.usuarioNombre
            tvTimeAgo.text = comentario.fecha
            tvCommentText.text = comentario.texto
            tvLikeCount.text = comentario.likes.toString()

            // Configurar texto de respuestas
            val cantidadRespuestas = comentario.respuestas.size
            tvRepliesCount.text = when {
                cantidadRespuestas == 0 -> "Sin respuestas"
                cantidadRespuestas == 1 -> "1 respuesta"
                else -> "$cantidadRespuestas respuestas"
            }

            // Ocultar botón si no hay respuestas
            btnToggleReplies.visibility = if (cantidadRespuestas > 0) View.VISIBLE else View.GONE

            // TODO: Cargar foto del usuario con Glide
            // Glide.with(itemView.context).load(comentario.usuarioFoto).into(ivUserAvatar)

            // Click en like
            btnLike.setOnClickListener {
                onLikeClick(comentario)
            }

            // Toggle respuestas
            btnToggleReplies.setOnClickListener {
                respuestasVisible = !respuestasVisible

                if (respuestasVisible) {
                    // Mostrar respuestas
                    layoutReplies.visibility = View.VISIBLE
                    ivArrowReplies.rotation = -90f // Flecha hacia arriba

                    // Configurar RecyclerView de respuestas
                    val respuestasAdapter = RespuestasAdapter(comentario.respuestas)
                    recyclerReplies.layoutManager = LinearLayoutManager(itemView.context)
                    recyclerReplies.adapter = respuestasAdapter
                } else {
                    // Ocultar respuestas
                    layoutReplies.visibility = View.GONE
                    ivArrowReplies.rotation = 90f // Flecha hacia abajo
                }
            }

            // Estado inicial
            layoutReplies.visibility = View.GONE
            ivArrowReplies.rotation = 90f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comentario_estruc, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        holder.bind(comentarios[position])
    }

    override fun getItemCount(): Int = comentarios.size

    fun updateComentarios(nuevosComentarios: List<Comentario>) {
        comentarios = nuevosComentarios
        notifyDataSetChanged()
    }
}