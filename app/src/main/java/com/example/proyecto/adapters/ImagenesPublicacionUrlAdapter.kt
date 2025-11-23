package com.example.proyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.proyecto.R

/**
 * Adapter para cargar imágenes desde URLs en un ViewPager2
 * con spinner de carga animado
 */
class ImagenesPublicacionUrlAdapter(
    private val imagenesUrls: List<String>
) : RecyclerView.Adapter<ImagenesPublicacionUrlAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imgPublicacion)
        val loadingSpinner: ProgressBar = itemView.findViewById(R.id.loadingSpinner)

        fun bind(imageUrl: String) {
            println("🖼️ Cargando imagen en ViewPager: $imageUrl")

            // Mostrar el spinner mientras carga
            loadingSpinner.visibility = View.VISIBLE

            Glide.with(itemView.context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        println("❌ Error cargando imagen en ViewPager: $imageUrl")
                        println("❌ Excepción: ${e?.message}")
                        e?.rootCauses?.forEach { cause ->
                            println("❌ Causa: ${cause.message}")
                        }

                        // Ocultar el spinner aunque haya error
                        loadingSpinner.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        println("✅ Imagen cargada exitosamente en ViewPager desde: $dataSource")

                        // Ocultar el spinner cuando la imagen se cargó
                        loadingSpinner.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_publicacion, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imagenesUrls[position])
    }

    override fun getItemCount(): Int = imagenesUrls.size
}