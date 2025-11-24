package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.proyecto.R
import com.example.proyecto.models.Publicacion

class PublicacionesPerfilAdapter(
    var publicaciones: List<Publicacion>,
    private val idUsuarioActual: String,
    private val onLikeClick: (Publicacion) -> Unit,
    private val onDislikeClick: (Publicacion) -> Unit,
    private val onCommentClick: (Publicacion) -> Unit,
    private val onFavoriteClick: (Publicacion) -> Unit,
    private val onPublicacionClick: (Publicacion) -> Unit,
    private val onEditarClick: (Publicacion) -> Unit,
    private val onEliminarClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesPerfilAdapter.PublicacionViewHolder>() {

    inner class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewPagerImagenes: ViewPager2 = itemView.findViewById(R.id.viewPagerImagenes)
        val layoutIndicadores: LinearLayout = itemView.findViewById(R.id.layoutIndicadores)
        val imgUsuarioPerfil: ImageView = itemView.findViewById(R.id.imgUsuarioPerfil)
        val txtUsuarioNombre: TextView = itemView.findViewById(R.id.txtUsuarioNombre)
        val btnMenu: ImageButton = itemView.findViewById(R.id.btnMenu)
        val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        val badgeBorrador: TextView = itemView.findViewById(R.id.badgeBorrador)
        val txtDescripcion: TextView = itemView.findViewById(R.id.txtDescripcion)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val txtLikes: TextView = itemView.findViewById(R.id.txtLikes)
        val btnDislike: ImageView = itemView.findViewById(R.id.btnDislike)
        val txtDislikes: TextView = itemView.findViewById(R.id.txtDislikes)
        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        val txtComments: TextView = itemView.findViewById(R.id.txtComments)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
        val layoutAcciones: LinearLayout = itemView.findViewById(R.id.layoutAcciones)

        fun bind(publicacion: Publicacion) {
            // Detectar si es un borrador
            val esBorrador = publicacion.id.startsWith("draft_")

            // Configurar usuario
            txtUsuarioNombre.text = publicacion.usuarioNombre

            // 🔍 DEBUG: Imprimir información de la foto de perfil
            println("👤 Cargando foto de perfil para: ${publicacion.usuarioNombre}")
            println("   URL foto: ${publicacion.usuarioFoto}")

            // Cargar foto de perfil del usuario
            if (!publicacion.usuarioFoto.isNullOrEmpty()) {
                println("   ✅ Cargando con Glide: ${publicacion.usuarioFoto}")
                Glide.with(itemView.context)
                    .load(publicacion.usuarioFoto)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(imgUsuarioPerfil)
            } else {
                println("   ⚠️ Sin foto, usando imagen por defecto")
                imgUsuarioPerfil.setImageResource(R.drawable.user)
            }

            txtTitulo.text = publicacion.titulo
            txtDescripcion.text = publicacion.descripcion
            txtFecha.text = publicacion.fecha

            // Mostrar/ocultar badge de borrador
            if (esBorrador) {
                badgeBorrador.visibility = View.VISIBLE
            } else {
                badgeBorrador.visibility = View.GONE
            }

            // Ocultar/mostrar botones de interacción según si es borrador
            if (esBorrador) {
                layoutAcciones.visibility = View.GONE
            } else {
                layoutAcciones.visibility = View.VISIBLE
                txtLikes.text = publicacion.likes.toString()
                txtDislikes.text = publicacion.dislikes.toString()
                txtComments.text = publicacion.comentarios.toString()

                // Configurar apariencia del botón Like
                if (publicacion.usuarioLike) {
                    btnLike.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.like_activo)
                    )
                } else {
                    btnLike.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.white)
                    )
                }

                // Configurar apariencia del botón Dislike
                if (publicacion.usuarioDislike) {
                    btnDislike.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.dislike_activo)
                    )
                } else {
                    btnDislike.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.white)
                    )
                }

                // Configurar apariencia del botón Favorito
                if (publicacion.usuarioFavorito) {
                    btnFavorite.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.favorito_activo)
                    )
                } else {
                    btnFavorite.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.white)
                    )
                }

                // Listeners de acciones
                btnLike.setOnClickListener { onLikeClick(publicacion) }
                btnDislike.setOnClickListener { onDislikeClick(publicacion) }
                btnComment.setOnClickListener { onCommentClick(publicacion) }
                btnFavorite.setOnClickListener { onFavoriteClick(publicacion) }
            }

            // Configurar ViewPager para imágenes
            if (publicacion.imagenesUrl.isNotEmpty()) {
                println("   🖼️ Cargando ${publicacion.imagenesUrl.size} imágenes de publicación")

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

            // Configurar menú de opciones (solo si es del usuario actual)
            if (publicacion.usuarioId == idUsuarioActual) {
                btnMenu.visibility = View.VISIBLE
                btnMenu.setOnClickListener { view ->
                    mostrarMenuOpciones(view, publicacion)
                }
            } else {
                btnMenu.visibility = View.GONE
            }

            itemView.setOnClickListener { onPublicacionClick(publicacion) }
        }

        private fun mostrarMenuOpciones(view: View, publicacion: Publicacion) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_publicacion, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_editar -> {
                        onEditarClick(publicacion)
                        true
                    }
                    R.id.menu_eliminar -> {
                        mostrarDialogoEliminar(publicacion)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        private fun mostrarDialogoEliminar(publicacion: Publicacion) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Eliminar publicación")
                .setMessage("¿Estás seguro de que deseas eliminar \"${publicacion.titulo}\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { dialog, _ ->
                    onEliminarClick(publicacion)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
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
            .inflate(R.layout.item_publicacion_perfil, parent, false)
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