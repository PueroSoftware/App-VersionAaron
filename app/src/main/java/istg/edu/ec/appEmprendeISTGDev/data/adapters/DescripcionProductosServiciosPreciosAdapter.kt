package istg.edu.ec.appEmprendeISTGDev.data.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import istg.edu.ec.appEmprendeISTGDev.R
import istg.edu.ec.appEmprendeISTGDev.data.model.ProductoServicioPrecioModel

// Adaptador para mostrar una lista de productos o servicios con sus precios en un RecyclerView
class DescripcionProductosServiciosPreciosAdapter(
    private val productos: List<ProductoServicioPrecioModel> // Lista de productos o servicios con precios
) : RecyclerView.Adapter<DescripcionProductosServiciosPreciosAdapter.ProductViewHolder>() {

    // Aquí guardamos qué productos están seleccionados
    private val seleccionados = mutableSetOf<ProductoServicioPrecioModel>()

    fun obtenerSeleccionados(): List<ProductoServicioPrecioModel> = seleccionados.toList()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombreProducto: TextView = itemView.findViewById(R.id.tvNombreProducto)
        private val precioProducto: TextView = itemView.findViewById(R.id.tvPrecioProducto)
        private val checkProducto: CheckBox = itemView.findViewById(R.id.checkProducto)

        // Método para asignar los valores de un producto a los elementos de la interfaz
        fun bind(producto: ProductoServicioPrecioModel) {
            nombreProducto.text = producto.nombreProductoServicio
            precioProducto.text = "$${String.format("%.2f", producto.precioProductoServicio)}"

            // Mantiene el estado del checkbox al reciclar vistas
            checkProducto.setOnCheckedChangeListener(null)
            checkProducto.isChecked = seleccionados.contains(producto)

            // Escucha los cambios del check
            checkProducto.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) seleccionados.add(producto)
                else seleccionados.remove(producto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.productos_servicios_revisar, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size
}
