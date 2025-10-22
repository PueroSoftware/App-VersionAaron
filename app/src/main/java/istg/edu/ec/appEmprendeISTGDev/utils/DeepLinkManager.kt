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

    // --- CONFIGURACIÓN ACTUALIZADA ---
    // URL de tu página de redirección en Google Sites.
    private const val WEB_REDIRECT_URL = "https://emprende-perfil.netlify.app"

    /**
     * Construye un enlace web universal que apunta a tu página de Google Sites.
     * Este enlace SIEMPRE será clickeable en cualquier aplicación (WhatsApp, etc.).
     * @param userId El ID del usuario para incluir en el enlace.
     * @return Una URL como "https://sites.google.com/.../home?perfil=USER_ID"
     */
    fun buildWebRedirectUrl(userId: String): String {
        // Añadimos el userId como un parámetro de consulta llamado "perfil"
        return "$WEB_REDIRECT_URL?perfil=$userId"
    }
    // --- FIN DE LA CONFIGURACIÓN ---

    // El esquema personalizado sigue siendo VITAL. La página web lo usa para abrir la app.
    fun buildCustomScheme(userId: String): String {
        return "app://profile/$userId"
    }

    fun buildPlayStoreUrl(context: Context): String {
        return "https://play.google.com/store/apps/details?id=${context.packageName}"
    }

    /**
     * Genera el texto plano para compartir. Ahora es mucho más limpio y efectivo.
     */
    private fun buildShareText(displayName: String?, userId: String): String {
        val webUrl = buildWebRedirectUrl(userId)
        return """
            Mira el perfil de ${displayName ?: "este usuario"} en EmprendeISTG:

            $webUrl

            Si no tienes la app, el enlace te ayudará a descargarla.
        """.trimIndent()
    }

    /**
     * Genera la versión HTML del texto para compartir, con un enlace limpio.
     */
    private fun buildShareHtml(displayName: String?, userId: String): String {
        val webUrl = buildWebRedirectUrl(userId)
        val title = "Abre el perfil de ${displayName ?: "este usuario"}"
        val linkText = "Abrir el perfil de ${displayName ?: "usuario"}"

        return """
            <p>$title</p>
            <p><b><a href="$webUrl">$linkText</a></b></p>
            <p>Si no tienes la app, el enlace te guiará para descargarla.</p>
        """.trimIndent()
    }

    /**
     * Devuelve un Intent que prioriza HTML pero incluye un fallback de texto plano.
     * Usa automáticamente los nuevos métodos de construcción de URL.
     */
    fun getShareIntent(context: Context, displayName: String?, userId: String): Intent {
        val shareText = buildShareText(displayName, userId)
        val shareHtml = buildShareHtml(displayName, userId)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_SUBJECT, "Perfil de ${displayName ?: "usuario"}")
            putExtra(Intent.EXTRA_HTML_TEXT, shareHtml)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    // El resto de tus funciones no necesitan cambios.
    // Funcionarán correctamente con esta nueva estrategia.

    fun shareProfile(context: Context, displayName: String?, userId: String) {
        val intent = getShareIntent(context, displayName, userId)
        try {
            context.startActivity(Intent.createChooser(intent, "Compartir perfil"))
        } catch (_: ActivityNotFoundException) {
            val browser = Intent(Intent.ACTION_VIEW, buildPlayStoreUrl(context).toUri())
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browser)
        }
    }

    fun handleIncomingIntent(
        activity: FragmentActivity,
        intent: Intent?,
        navHostResId: Int? = null,
        perfilDestinationId: Int? = null,
        argName: String = "userId"
    ): Boolean {
        if (intent == null) return false

        val data: Uri? = intent.data
        var userId: String? = null

        if (data != null) {
            userId = extractUserIdFromUri(data)
        }

        if (userId.isNullOrBlank() && intent.extras != null) {
            userId = intent.extras?.getString(argName)
        }

        if (userId.isNullOrBlank() && data != null) {
            userId = data.getQueryParameter(argName)
        }

        if (userId.isNullOrBlank()) return false

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
                val navHostFragment = activity.supportFragmentManager.fragments.firstOrNull { it is NavHostFragment } as? NavHostFragment
                navController = navHostFragment?.navController
            }
        } catch (_: Exception) {
            // Error finding NavController, proceed gracefully
        }

        if (navController == null) {
            return false
        }

        return try {
            if (perfilDestinationId != null) {
                val bundle = Bundle().apply { putString(argName, userId) }
                navController.navigate(perfilDestinationId, bundle)
                true
            } else {
                val deepLinkIntent = Intent(Intent.ACTION_VIEW, buildCustomScheme(userId).toUri())
                navController.handleDeepLink(deepLinkIntent)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun extractUserIdFromUri(uri: Uri): String? {
        try {
            val host = uri.host ?: ""
            val pathSegments = uri.pathSegments ?: emptyList()

            if (host.equals("profile", ignoreCase = true) && pathSegments.isNotEmpty()) {
                return pathSegments[0]
            }

            if (pathSegments.size >= 2 && pathSegments[0].equals("profile", ignoreCase = true)) {
                return pathSegments[1]
            }

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
