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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.models.Comentario

class ComentariosAdapter(
    private var comentarios: List<Comentario>,
    private val onLikeClick: (Comentario) -> Unit,
    private val onSendReply: (Comentario, String) -> Unit
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
        val replyInputCard: CardView = itemView.findViewById(R.id.replyInputCard)
        val etReply: EditText = itemView.findViewById(R.id.etReply)
        val btnSendReply: ImageButton = itemView.findViewById(R.id.btnSendReply)

        private var respuestasVisible = false

        fun bind(comentario: Comentario) {
            tvUserName.text = comentario.usuarioNombre
            tvTimeAgo.text = comentario.fecha
            tvCommentText.text = comentario.texto
            tvLikeCount.text = comentario.likes.toString()

            // ✅ Configurar estado visual del botón like
            if (comentario.tieneLike) {
                btnLike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.like_activo)
                )
            } else {
                btnLike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
            }

            // ✅ CAMBIO: Configurar texto de respuestas - SIEMPRE VISIBLE
            val cantidadRespuestas = comentario.respuestas.size
            tvRepliesCount.text = when {
                cantidadRespuestas == 0 -> "Responder"  // 🔥 Cambio aquí
                cantidadRespuestas == 1 -> "1 respuesta"
                else -> "$cantidadRespuestas respuestas"
            }

            // ✅ CAMBIO: SIEMPRE mostrar el botón (antes estaba oculto)
            btnToggleReplies.visibility = View.VISIBLE

            // ✅ Cargar foto del usuario con Glide
            if (!comentario.usuarioFoto.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(comentario.usuarioFoto)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(ivUserAvatar)
            } else {
                ivUserAvatar.setImageResource(R.drawable.user)
            }

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
                } else {
                    Toast.makeText(itemView.context, "Escribe una respuesta", Toast.LENGTH_SHORT).show()
                }
            }

            // Estado inicial - respuestas ocultas
            layoutReplies.visibility = View.GONE
            ivArrowReplies.rotation = 90f
            respuestasVisible = false
        }

        private fun toggleRespuestas(comentario: Comentario) {
            respuestasVisible = !respuestasVisible

            if (respuestasVisible) {
                // Mostrar sección de respuestas e input
                layoutReplies.visibility = View.VISIBLE
                ivArrowReplies.rotation = -90f

                // ✅ Solo configurar RecyclerView si HAY respuestas
                if (comentario.respuestas.isNotEmpty()) {
                    recyclerReplies.visibility = View.VISIBLE
                    val respuestasAdapter = RespuestasAdapter(comentario.respuestas)
                    recyclerReplies.layoutManager = LinearLayoutManager(itemView.context)
                    recyclerReplies.adapter = respuestasAdapter
                } else {
                    // Si no hay respuestas, ocultar el RecyclerView
                    recyclerReplies.visibility = View.GONE
                }
            } else {
                // Ocultar respuestas e input
                layoutReplies.visibility = View.GONE
                ivArrowReplies.rotation = 90f
                etReply.text.clear()
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