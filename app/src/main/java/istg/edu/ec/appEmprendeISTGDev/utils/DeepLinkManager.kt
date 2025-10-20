package istg.edu.ec.appEmprendeISTGDev.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import java.lang.Exception

object DeepLinkManager {

    // Construye el intent:// y esquema app://
    fun buildIntentUri(context: Context, userId: String): String {
        val pkg = context.packageName
        return "intent://profile/$userId#Intent;scheme=app;package=$pkg;end"
    }

    fun buildCustomScheme(userId: String): String {
        return "app://profile/$userId"
    }

    fun buildPlayStoreUrl(context: Context): String {
        return "https://play.google.com/store/apps/details?id=${context.packageName}"
    }

    // Texto para compartir (incluye intent://, esquema alternativo y Play Store)
    fun buildShareText(context: Context, displayName: String?, userId: String): String {
        val intentUri = buildIntentUri(context, userId)
        val custom = buildCustomScheme(userId)
        val ps = buildPlayStoreUrl(context)
        return """
            Abre el perfil de ${displayName ?: "este usuario"}:

            $intentUri

            Si el enlace anterior no funciona, prueba:
            $custom

            Si no tienes la app, descárgala aquí:
            $ps
        """.trimIndent()
    }

    // Devuelve un Intent listable para compartir (para usar desde un Adapter/ViewHolder)
    fun getShareIntent(context: Context, displayName: String?, userId: String): Intent {
        val shareText = buildShareText(context, displayName, userId)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Perfil de ${displayName ?: "usuario"}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    // Helper que lanza el chooser de compartir (UI convenience)
    fun shareProfile(context: Context, displayName: String?, userId: String) {
        val intent = getShareIntent(context, displayName, userId)
        try {
            context.startActivity(Intent.createChooser(intent, "Compartir perfil"))
        } catch (_: ActivityNotFoundException) {
            // Raro: no hay apps para compartir; abrir Play Store como mínimo fallback
            val browser = Intent(Intent.ACTION_VIEW, buildPlayStoreUrl(context).toUri())
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browser)
        }
    }

    /**
     * Procesa un Intent entrante: extrae userId y navega al fragment perfil.
     * - activity: la Activity que contiene el NavHostFragment.
     * - intent: el intent entrante.
     * - navHostResId: optional id del NavHostFragment (ej. R.id.nav_host_fragment). Si null, intenta buscar un NavHostFragment en el FragmentManager.
     * - perfilDestinationId: optional id del destino Perfil en tu nav graph (ej. R.id.perfilFragment). Si es null, intentará manejar el deep link con NavController.handleDeepLink.
     * - argName: nombre del argumento userId en tu nav graph (por defecto "userId").
     *
     * Devuelve true si logró navegar / manejar el intent; false si no encontró userId o no pudo navegar.
     */
    fun handleIncomingIntent(
        activity: FragmentActivity,
        intent: Intent?,
        navHostResId: Int? = null,
        perfilDestinationId: Int? = null,
        argName: String = "userId"
    ): Boolean {
        if (intent == null) return false

        // 1) intentar extraer userId desde data Uri
        val data: Uri? = intent.data
        var userId: String? = null

        if (data != null) {
            userId = extractUserIdFromUri(data)
        }

        // 2) fallback: extras
        if (userId.isNullOrBlank() && intent.extras != null) {
            userId = intent.extras?.getString(argName)
        }

        // 3) segunda fallback: query parameter ?userId=123
        if (userId.isNullOrBlank() && data != null) {
            userId = data.getQueryParameter(argName)
        }

        if (userId.isNullOrBlank()) return false

        // 4) Obtener NavController: usar navHostResId si se proporcionó; si no, buscar NavHostFragment en el FragmentManager
        var navController: NavController? = null
        try {
            val navHostId = navHostResId ?: run {
                val fragments = activity.supportFragmentManager.fragments
                val navHostFragment = fragments.firstOrNull { it is NavHostFragment } as? NavHostFragment
                navHostFragment?.id
            }

            if (navHostId != null) {
                navController = Navigation.findNavController(activity, navHostId)
            } else {
                // no se encontró navHostId -> intentar buscar NavHostFragment y obtener su navController
                val navHostFragment = activity.supportFragmentManager.fragments.firstOrNull { it is NavHostFragment } as? NavHostFragment
                navController = navHostFragment?.navController
            }
        } catch (_: Exception) {
            // Error finding NavController, proceed gracefully
        }

        if (navController == null) {
            return false
        }

        // 5) Navegar:
        return try {
            if (perfilDestinationId != null) {
                // Navegar por id (se pasa el argumento)
                val bundle = Bundle().apply { putString(argName, userId) }
                navController.navigate(perfilDestinationId, bundle)
                true
            } else {
                // Si no proporcionaron un destino id, usamos el deep link (requiere que el nav graph tenga el deepLink definido)
                val deepLinkIntent = Intent(Intent.ACTION_VIEW, buildCustomScheme(userId).toUri())
                navController.handleDeepLink(deepLinkIntent)
            }
        } catch (_: Exception) {
            false
        }
    }

    // Extrae userId de URIs esperadas:
    // - app://profile/{userId}
    // - intent://profile/{userId}
    // - https://tudominio/profile/{userId}
    private fun extractUserIdFromUri(uri: Uri): String? {
        try {
            val host = uri.host ?: ""
            val pathSegments = uri.pathSegments ?: emptyList()
            // caso: app://profile/123 -> host == "profile", pathSegments[0] == "123"
            if (host.equals("profile", ignoreCase = true) && pathSegments.isNotEmpty()) {
                return pathSegments[0]
            }
            // caso: https://tudominio/profile/123 -> pathSegments[0] == "profile", pathSegments[1] == "123"
            if (pathSegments.size >= 2 && pathSegments[0].equals("profile", ignoreCase = true)) {
                return pathSegments[1]
            }
            // caso: intent://profile/123 -> pathSegments puede ser ["profile","123"]
            if (pathSegments.isNotEmpty()) {
                val idx = pathSegments.indexOfFirst { it.equals("profile", ignoreCase = true) }
                if (idx >= 0 && pathSegments.size > idx + 1) {
                    return pathSegments[idx + 1]
                }
                if (pathSegments.size == 1) {
                    return pathSegments[0]
                }
            }
        } catch (_: Exception) {
            // Could not parse, fallback to lastPathSegment
        }
        val last = uri.lastPathSegment
        if (!last.isNullOrBlank()) return last
        return null
    }
}