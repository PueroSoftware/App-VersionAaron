package istg.edu.ec.appEmprendeISTGDev.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import istg.edu.ec.appEmprendeISTGDev.R
import istg.edu.ec.appEmprendeISTGDev.data.adapters.LinksProductosServiciosAdapter
import istg.edu.ec.appEmprendeISTGDev.data.model.AgregarNegocioModel
import istg.edu.ec.appEmprendeISTGDev.ui.fragments.ProductoResumenDialogFragment
import istg.edu.ec.appEmprendeISTGDev.utils.DeepLinkManager

// Adaptador para mostrar una lista de negocios/publicaciones en el RecyclerView del Home
class HomeAdapter(private var items: List<AgregarNegocioModel>) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreNegocio: TextView = view.findViewById(R.id.tvNombreNegocio)
        val propietario: TextView = view.findViewById(R.id.tvVisitarPerfil)
        val descripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val ubicacion: TextView = view.findViewById(R.id.tvUbicacion)
        val rvLinksProductosServicios: RecyclerView = view.findViewById(R.id.rvLinksProductosServicios)
        val lyProductos: LinearLayout? = view.findViewById(R.id.lyProductos)
        val btnCompartir: ImageButton? = view.findViewById(R.id.btnCompartir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val publicacionActual = items[position]

        with(holder) {
            nombreNegocio.text = publicacionActual.nombreLocal.ifBlank { "Sin nombre" }
            propietario.text = publicacionActual.nombreUsuario.ifBlank { "Sin propietario" }
            descripcion.text = publicacionActual.descripcion.ifBlank { "Sin descripción" }
            ubicacion.text = publicacionActual.direccion.ifBlank { "Sin dirección" }

            rvLinksProductosServicios.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = LinksProductosServiciosAdapter(publicacionActual.enlacesProductos ?: emptyList())
            }

            lyProductos?.setOnClickListener {
                val dialog = ProductoResumenDialogFragment(
                    productos = publicacionActual.descripcionProductosServicios ?: emptyList(),
                    numeroWhatsApp = publicacionActual.telefonoWhatsApp ?: ""
                )

                val activity = it.context as? AppCompatActivity
                activity?.let { act ->
                    dialog.show(act.supportFragmentManager, "ProductosDialog")
                }
            }

            // --- Listener de compartir (Versión Final y Corregida) ---
            btnCompartir?.setOnClickListener {
                val context = itemView.context
                val userId = publicacionActual.uid
                val publicacionId = publicacionActual.id

                if (userId != null && publicacionId != null) {
                    // Llamamos a la función correcta con los IDs de esta publicación
                    DeepLinkManager.sharePublication(context, userId, publicacionId)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<AgregarNegocioModel>) {
        items = newList
        notifyDataSetChanged()
    }
}
