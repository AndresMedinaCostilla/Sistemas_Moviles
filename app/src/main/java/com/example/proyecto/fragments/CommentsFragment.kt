package com.example.proyecto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.adapters.ComentariosAdapter
import com.example.proyecto.models.Comentario
import com.example.proyecto.models.Respuesta

class CommentsFragment : Fragment() {

    private lateinit var recyclerComentarios: RecyclerView
    private lateinit var adapter: ComentariosAdapter
    private lateinit var etComment: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        recyclerComentarios = view.findViewById(R.id.recyclerComentarios)
        etComment = view.findViewById(R.id.etComment)
        btnSend = view.findViewById(R.id.btnSend)
        btnBack = view.findViewById(R.id.btnBack)

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar botones
        setupButtons()

        // Cargar comentarios de ejemplo
        cargarComentarios()
    }

    private fun setupRecyclerView() {
        adapter = ComentariosAdapter(
            comentarios = emptyList(),
            onLikeClick = { comentario ->
                // Manejar like
                Toast.makeText(context, "Like en comentario de ${comentario.usuarioNombre}", Toast.LENGTH_SHORT).show()
                // TODO: Actualizar likes en la base de datos
            }
        )

        recyclerComentarios.layoutManager = LinearLayoutManager(context)
        recyclerComentarios.adapter = adapter
    }

    private fun setupButtons() {
        // Botón volver
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón enviar comentario
        btnSend.setOnClickListener {
            val texto = etComment.text.toString().trim()
            if (texto.isNotEmpty()) {
                // TODO: Enviar comentario a la base de datos
                Toast.makeText(context, "Comentario enviado: $texto", Toast.LENGTH_SHORT).show()
                etComment.text.clear()

                // Aquí agregarías el nuevo comentario a la lista y actualizarías el adapter
            } else {
                Toast.makeText(context, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarComentarios() {
        // DATOS DE EJEMPLO - Reemplaza con tu consulta a la base de datos
        val comentariosEjemplo = listOf(
            Comentario(
                id = "1",
                usuarioId = "user1",
                usuarioNombre = "María García",
                usuarioFoto = "",
                texto = "¡Excelente publicación! Me encantó el contenido, muy informativo y bien estructurado.",
                fecha = "22 de nov. 2025, 2:30 PM",
                likes = 15,
                publicacionId = "pub1",
                respuestas = mutableListOf(
                    Respuesta(
                        id = "r1",
                        usuarioId = "user2",
                        usuarioNombre = "Carlos Ruiz",
                        usuarioFoto = "",
                        texto = "Totalmente de acuerdo contigo María!",
                        fecha = "22 de nov. 2025, 3:15 PM",
                        comentarioId = "1"
                    ),
                    Respuesta(
                        id = "r2",
                        usuarioId = "user3",
                        usuarioNombre = "Ana López",
                        usuarioFoto = "",
                        texto = "Sí, el autor hizo un gran trabajo explicando todo.",
                        fecha = "22 de nov. 2025, 3:45 PM",
                        comentarioId = "1"
                    )
                )
            ),
            Comentario(
                id = "2",
                usuarioId = "user4",
                usuarioNombre = "Pedro Martínez",
                usuarioFoto = "",
                texto = "Interesante punto de vista, aunque discrepo en algunos detalles. ¿Podrías explicar más sobre...?",
                fecha = "22 de nov. 2025, 4:00 PM",
                likes = 8,
                publicacionId = "pub1",
                respuestas = mutableListOf(
                    Respuesta(
                        id = "r3",
                        usuarioId = "user5",
                        usuarioNombre = "Laura Fernández",
                        usuarioFoto = "",
                        texto = "También tengo esa duda, espero que el autor responda.",
                        fecha = "22 de nov. 2025, 4:30 PM",
                        comentarioId = "2"
                    )
                )
            ),
            Comentario(
                id = "3",
                usuarioId = "user6",
                usuarioNombre = "Roberto Sánchez",
                usuarioFoto = "",
                texto = "Gracias por compartir, esto me ayudó mucho con mi proyecto.",
                fecha = "22 de nov. 2025, 5:15 PM",
                likes = 23,
                publicacionId = "pub1",
                respuestas = mutableListOf()
            ),
            Comentario(
                id = "4",
                usuarioId = "user7",
                usuarioNombre = "Sofia Ramírez",
                usuarioFoto = "",
                texto = "¿Alguien tiene más información sobre este tema? Me gustaría profundizar más.",
                fecha = "22 de nov. 2025, 6:00 PM",
                likes = 5,
                publicacionId = "pub1",
                respuestas = mutableListOf(
                    Respuesta(
                        id = "r4",
                        usuarioId = "user8",
                        usuarioNombre = "Diego Torres",
                        usuarioFoto = "",
                        texto = "Te recomiendo buscar en la biblioteca, hay varios libros sobre esto.",
                        fecha = "22 de nov. 2025, 6:20 PM",
                        comentarioId = "4"
                    ),
                    Respuesta(
                        id = "r5",
                        usuarioId = "user9",
                        usuarioNombre = "Valentina Cruz",
                        usuarioFoto = "",
                        texto = "También puedes ver el canal de YouTube 'TechExplained', tiene buenos videos.",
                        fecha = "22 de nov. 2025, 6:45 PM",
                        comentarioId = "4"
                    ),
                    Respuesta(
                        id = "r6",
                        usuarioId = "user1",
                        usuarioNombre = "María García",
                        usuarioFoto = "",
                        texto = "¡Gracias por las recomendaciones!",
                        fecha = "22 de nov. 2025, 7:00 PM",
                        comentarioId = "4"
                    )
                )
            )
        )

        // Actualizar adapter
        adapter.updateComentarios(comentariosEjemplo)

        // TODO: Cuando conectes a tu base de datos, reemplaza esto por:
        /*
        viewModel.obtenerComentarios(publicacionId).observe(viewLifecycleOwner) { comentarios ->
            adapter.updateComentarios(comentarios)
        }
        */
    }
}