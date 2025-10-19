package istg.edu.ec.appEmprendeISTGDev.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import istg.edu.ec.appEmprendeISTGDev.R
import istg.edu.ec.appEmprendeISTGDev.data.adapters.DescripcionProductosServiciosPreciosAdapter
import istg.edu.ec.appEmprendeISTGDev.data.adapters.LinksExternosAdapter
import istg.edu.ec.appEmprendeISTGDev.data.adapters.LinksProductosServiciosAdapter
import istg.edu.ec.appEmprendeISTGDev.data.model.AgregarNegocioModel
import istg.edu.ec.appEmprendeISTGDev.ui.fragments.ProductoResumenDialogFragment

// Adaptador para mostrar una lista de negocios en el RecyclerView del Home
class HomeAdapter(private var items: List<AgregarNegocioModel>) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    // ViewHolder que representa cada negocio en la lista
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Elementos de la UI dentro de cada ítem del RecyclerView
        val nombreNegocio: TextView = view.findViewById(R.id.tvNombreNegocio)
        val propietario: TextView = view.findViewById(R.id.tvVisitarPerfil)
        val descripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val ubicacion: TextView = view.findViewById(R.id.tvUbicacion)
//        val rvProductosServicios: RecyclerView = view.findViewById(R.id.rvProductosServiciosPrecios)
        val rvLinksProductosServicios: RecyclerView = view.findViewById(R.id.rvLinksProductosServicios)
        val lyProductos: LinearLayout? = view.findViewById(R.id.lyProductos)
    }

    // Infla el diseño del ítem y crea un nuevo ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)
        return ViewHolder(view)
    }

    // Vincula los datos del negocio en la posición actual con el ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val negocio = items[position]

        with(holder) {
            // Asignar valores a los elementos de la UI
            nombreNegocio.text = negocio.nombreLocal ?: "Sin nombre"
            propietario.text = negocio.nombreUsuario ?: "Sin propietario"
            descripcion.text = negocio.descripcion ?: "Sin descripción"
            ubicacion.text = negocio.direccion ?: "Sin dirección"

            // Configurar RecyclerView de productos (si quisieras mantenerlo reducido)
//            rvProductosServicios.apply {
//                layoutManager = LinearLayoutManager(context)
//                adapter = DescripcionProductosServiciosPreciosAdapter(
//                    negocio.descripcionProductosServicios ?: emptyList()
//                )
//            }

            // Configurar RecyclerView de links
            rvLinksProductosServicios.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = LinksProductosServiciosAdapter(
                    negocio.enlacesProductos ?: emptyList()
                )
            }

            // 👉 Listener para abrir el diálogo de productos y numeroWhatsApp
            lyProductos?.setOnClickListener {
                val dialog = ProductoResumenDialogFragment(
                    productos = negocio.descripcionProductosServicios ?: emptyList(),
                    numeroWhatsApp = negocio.telefonoWhatsApp ?: "" // Aquí pasas el número del negocio
                )

//                val dialog = ProductoResumenDialogFragment.newInstance(
//                    negocio.descripcionProductosServicios ?: emptyList()
//                )

                val activity = it.context as? AppCompatActivity
                activity?.let { act ->
                    dialog.show(act.supportFragmentManager, "ProductosDialog")
                }
            }
        }
    }

    // Retorna el número total de elementos en la lista
    override fun getItemCount(): Int = items.size

    // Método para actualizar la lista de negocios y notificar cambios
    fun updateList(newList: List<AgregarNegocioModel>) {
        items = newList
        notifyDataSetChanged()
    }
}

//rvLinksProductosServicios.apply {
//    layoutManager = LinearLayoutManager(context)
//    adapter = LinksExternosAdapter(
//        negocio.enlacesExternos ?: emptyList()
//    )
//}
