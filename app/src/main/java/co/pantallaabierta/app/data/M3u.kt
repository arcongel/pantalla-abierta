package co.pantallaabierta.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Un canal leído de una lista M3U. */
data class M3uChannel(
    val name: String,
    val url: String,
    val logo: String,
    val group: String,
    val userAgent: String,
    val referrer: String,
)

/** Una lista que el usuario agregó (la app no trae ninguna). */
data class M3uList(val name: String, val url: String)

object M3u {

    suspend fun load(url: String): List<M3uChannel> = withContext(Dispatchers.IO) {
        val text = download(url)
        require(text.trimStart().startsWith("#EXTM3U", ignoreCase = true) || text.contains("#EXTINF")) {
            "No es una lista M3U."
        }
        parse(text)
    }

    fun parse(text: String): List<M3uChannel> {
        val out = mutableListOf<M3uChannel>()
        var info: String? = null
        var ua = ""
        var ref = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> { info = line; ua = ""; ref = "" }
                line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) ->
                    ua = line.substringAfter('=').trim()
                line.startsWith("#EXTVLCOPT:http-referrer=", ignoreCase = true) ->
                    ref = line.substringAfter('=').trim()
                line.startsWith("#") -> Unit
                else -> {
                    val i = info ?: ""
                    out += M3uChannel(
                        name = i.substringAfterLast(',', "").trim().ifBlank { line },
                        url = line,
                        logo = attr(i, "tvg-logo"),
                        group = attr(i, "group-title").ifBlank { "Otros" },
                        userAgent = ua.ifBlank { attr(i, "http-user-agent") },
                        referrer = ref.ifBlank { attr(i, "http-referrer") },
                    )
                    info = null
                }
            }
        }
        return out
    }

    private fun attr(line: String, key: String): String =
        Regex("$key=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1).orEmpty()

    private fun download(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "PantallaAbierta/1.0 (Android)")
        return try {
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}

/** Listas guardadas en el teléfono. */
class SavedLists(context: Context) {
    private val prefs = context.getSharedPreferences("listas_m3u", Context.MODE_PRIVATE)

    fun all(): List<M3uList> {
        val arr = JSONArray(prefs.getString("items", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            M3uList(o.getString("name"), o.getString("url"))
        }
    }

    fun add(list: M3uList): List<M3uList> = save(all().filter { it.url != list.url } + list)

    fun remove(list: M3uList): List<M3uList> = save(all().filter { it.url != list.url })

    private fun save(items: List<M3uList>): List<M3uList> {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
        prefs.edit().putString("items", arr.toString()).apply()
        return items
    }

    fun acceptedNotice(): Boolean = prefs.getBoolean("aviso", false)
    fun acceptNotice() = prefs.edit().putBoolean("aviso", true).apply()
}
