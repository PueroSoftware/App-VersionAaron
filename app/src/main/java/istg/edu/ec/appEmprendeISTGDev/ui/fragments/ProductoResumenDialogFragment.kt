package istg.edu.ec.appEmprendeISTGDev.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import istg.edu.ec.appEmprendeISTGDev.R
import istg.edu.ec.appEmprendeISTGDev.data.adapters.DescripcionProductosServiciosPreciosAdapter
import istg.edu.ec.appEmprendeISTGDev.data.model.ProductoServicioPrecioModel

class ProductoResumenDialogFragment(
    private val productos: List<ProductoServicioPrecioModel>,
    private val numeroWhatsApp: String // Sin valor por defecto - se pasa desde fuera
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Esto evita que el diálogo se destruya al rotar la pantalla
        retainInstance = true
    }

    private lateinit var adapter: DescripcionProductosServiciosPreciosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_producto_resumen_dialog, container, false)

        val rv = view.findViewById<RecyclerView>(R.id.rvProductosServiciosPrecios)
        val btnClose = view.findViewById<ImageButton>(R.id.btn_close)
        val btnEnviarWhatsApp = view.findViewById<ImageButton>(R.id.btnEnviarWhatsApp)
        val etNotas = view.findViewById<EditText>(R.id.etNotas)

        adapter = DescripcionProductosServiciosPreciosAdapter(productos)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnClose.setOnClickListener { dismiss() }

        btnEnviarWhatsApp.setOnClickListener {
            val seleccionados = adapter.obtenerSeleccionados()

            if (seleccionados.isEmpty()) {
                etNotas.error = "Selecciona al menos un producto"
                return@setOnClickListener
            }

            // Validar que el número de WhatsApp no esté vacío
            if (numeroWhatsApp.isBlank()) {
                etNotas.error = "Número de WhatsApp no disponible"
                return@setOnClickListener
            }

            val mensajeBase = etNotas.text.toString().ifBlank { "Deseo cotizar:" }
            val listaProductos = seleccionados.joinToString("\n") {
                "- ${it.nombreProductoServicio} $${String.format("%.2f", it.precioProductoServicio)}"
            }

            val mensajeFinal = "$mensajeBase\n$listaProductos"

            // Limpieza del número (elimina espacios o guiones)
            val numeroLimpio = numeroWhatsApp.replace(" ", "").replace("-", "")

            // Construcción del link directo a WhatsApp con número y mensaje
            val url = "https://wa.me/593${numeroLimpio.removePrefix("0")}?text=${Uri.encode(mensajeFinal)}"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}


//📲 Próximo paso
//
//Cuando ya tengas el número guardado en tu modelo o base de datos, solo se reemplaza esto:
//ProductoResumenDialogFragment(productos, negocio.telefono)
