package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.models.Comentario

class ComentariosAdapter(
    private var comentarios: List<Comentario>,
    private val onLikeClick: (Comentario) -> Unit,
    private val onSendReply: (Comentario, String) -> Unit  // Nuevo callback para enviar respuestas
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

        // Nuevos elementos para el input de respuesta
        val replyInputCard: CardView = itemView.findViewById(R.id.replyInputCard)
        val etReply: EditText = itemView.findViewById(R.id.etReply)
        val btnSendReply: ImageButton = itemView.findViewById(R.id.btnSendReply)

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
                toggleRespuestas(comentario)
            }

            // Enviar respuesta
            btnSendReply.setOnClickListener {
                val textoRespuesta = etReply.text.toString().trim()
                if (textoRespuesta.isNotEmpty()) {
                    onSendReply(comentario, textoRespuesta)
                    etReply.text.clear()
                    Toast.makeText(itemView.context, "Respuesta enviada", Toast.LENGTH_SHORT).show()
                    // TODO: Actualizar la lista de respuestas desde la base de datos
                } else {
                    Toast.makeText(itemView.context, "Escribe una respuesta", Toast.LENGTH_SHORT).show()
                }
            }

            // Estado inicial
            layoutReplies.visibility = View.GONE
            ivArrowReplies.rotation = 90f
        }

        private fun toggleRespuestas(comentario: Comentario) {
            respuestasVisible = !respuestasVisible

            if (respuestasVisible) {
                // Mostrar respuestas e input
                layoutReplies.visibility = View.VISIBLE
                ivArrowReplies.rotation = -90f // Flecha hacia arriba

                // Configurar RecyclerView de respuestas
                val respuestasAdapter = RespuestasAdapter(comentario.respuestas)
                recyclerReplies.layoutManager = LinearLayoutManager(itemView.context)
                recyclerReplies.adapter = respuestasAdapter
            } else {
                // Ocultar respuestas e input
                layoutReplies.visibility = View.GONE
                ivArrowReplies.rotation = 90f // Flecha hacia abajo
                etReply.text.clear() // Limpiar el input al cerrar
            }
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