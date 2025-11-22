package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.models.Respuesta

class RespuestasAdapter(
    private var respuestas: List<Respuesta>
) : RecyclerView.Adapter<RespuestasAdapter.RespuestaViewHolder>() {

    inner class RespuestaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivReplyAvatar: ImageView = itemView.findViewById(R.id.ivReplyAvatar)
        val tvReplyUserName: TextView = itemView.findViewById(R.id.tvReplyUserName)
        val tvReplyTime: TextView = itemView.findViewById(R.id.tvReplyTime)
        val tvReplyText: TextView = itemView.findViewById(R.id.tvReplyText)

        fun bind(respuesta: Respuesta) {
            tvReplyUserName.text = respuesta.usuarioNombre
            tvReplyTime.text = respuesta.fecha
            tvReplyText.text = respuesta.texto

            // TODO: Cargar foto del usuario con Glide
            // Glide.with(itemView.context).load(respuesta.usuarioFoto).into(ivReplyAvatar)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RespuestaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_respuesta, parent, false)
        return RespuestaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RespuestaViewHolder, position: Int) {
        holder.bind(respuestas[position])
    }

    override fun getItemCount(): Int = respuestas.size

    fun updateRespuestas(nuevasRespuestas: List<Respuesta>) {
        respuestas = nuevasRespuestas
        notifyDataSetChanged()
    }
}