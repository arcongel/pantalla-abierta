package co.pantallaabierta.app.data

import android.content.Context
import org.json.JSONObject

data class Channel(
    val id: String,
    val name: String,
    val category: String,
    val color: Long,
    val url: String,
    val storeQuery: String,
    /** ID del canal OFICIAL de YouTube; vacío si no tiene. */
    val youtubeChannel: String,
)

object ChannelRepository {

    /** Lee la lista de canales oficiales desde assets/channels.json. */
    fun load(context: Context): List<Channel> {
        val text = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("channels")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Channel(
                id = o.getString("id"),
                name = o.getString("name"),
                category = o.optString("category"),
                color = parseColor(o.optString("color", "#455A64")),
                url = o.getString("url"),
                storeQuery = o.optString("store_query", o.getString("name")),
                youtubeChannel = o.optString("youtube_channel"),
            )
        }
    }

    private fun parseColor(hex: String): Long {
        val clean = hex.removePrefix("#")
        val argb = if (clean.length == 6) "FF$clean" else clean
        return argb.toLongOrNull(16) ?: 0xFF455A64
    }
}

/** Favoritos guardados en el teléfono (sin cuentas ni servidores). */
class Favorites(context: Context) {
    private val prefs = context.getSharedPreferences("favoritos", Context.MODE_PRIVATE)

    fun all(): Set<String> = prefs.getStringSet("ids", emptySet()) ?: emptySet()

    fun toggle(id: String): Set<String> {
        val current = all().toMutableSet()
        if (!current.add(id)) current.remove(id)
        prefs.edit().putStringSet("ids", current).apply()
        return current
    }
}
