package istg.edu.ec.appEmprendeISTGDev.ui.activitys

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import istg.edu.ec.appEmprendeISTGDev.R
import istg.edu.ec.appEmprendeISTGDev.utils.setStatusBarColor

class Invitado : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invitado)

        setStatusBarColor(R.color.azul_navbar, lightIcons = true) // Fondo azul, íconos blancos

        val toolbar: Toolbar = findViewById(R.id.toolbarInvity)
        setSupportActionBar(toolbar)

        // Ahora puedes personalizar la Toolbar
        supportActionBar?.apply {
            title = "Explorar como invitado"
            setDisplayHomeAsUpEnabled(true)
            // setHomeAsUpIndicator(R.drawable.ic_back) // Si tienes un icono personalizado
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val invite = intent.getStringExtra("evento")
        val miBundle = Bundle()
        miBundle.putString("miParametro", invite)
        navegarARevisarPublicaciones(miBundle)
    }

    fun navegarARevisarPublicaciones(bundle: Bundle) {
        findNavController(R.id.nav_host_fragment).navigate(R.id.filtroBusquedaFragment, bundle)
    }

    // 🔙 Manejar clic en el botón de regreso de la Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
