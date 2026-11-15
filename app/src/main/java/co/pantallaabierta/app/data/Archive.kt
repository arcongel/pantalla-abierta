package co.pantallaabierta.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Catálogo de películas y series del Internet Archive (archive.org).
 *
 * Regla legal de la app: SOLO se muestran títulos que el propio archivo marca
 * con licencia de dominio público o Creative Commons. Todo lo demás se descarta,
 * aunque esté en el archivo.
 */
data class Title(
    val id: String,
    val name: String,
    val year: String,
    val license: String,
) {
    val thumbnail: String get() = "https://archive.org/services/img/$id"
}

data class Episode(val label: String, val url: String)

data class TitleDetail(
    val title: Title,
    val description: String,
    val episodes: List<Episode>,
)

data class Shelf(val name: String, val query: String)

object Archive {

    private const val LICENSE_FILTER =
        "(licenseurl:*publicdomain* OR licenseurl:*creativecommons*)"

    val shelves = listOf(
        Shelf("Películas en español", "mediatype:movies AND language:(spa OR Spanish OR español) AND $LICENSE_FILTER"),
        Shelf("Clásicos del cine", "mediatype:movies AND collection:feature_films AND $LICENSE_FILTER"),
        Shelf("Series clásicas", "mediatype:movies AND collection:classic_tv AND $LICENSE_FILTER"),
        Shelf("Animación", "mediatype:movies AND collection:animationandcartoons AND $LICENSE_FILTER"),
        Shelf("Cine mudo", "mediatype:movies AND collection:silent_films AND $LICENSE_FILTER"),
    )

    fun searchQuery(text: String): String {
        val safe = text.replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim()
        return "mediatype:movies AND title:($safe) AND $LICENSE_FILTER"
    }

    suspend fun list(query: String, rows: Int = 30): List<Title> = withContext(Dispatchers.IO) {
        val url = "https://archive.org/advancedsearch.php?q=" + enc(query) +
            "&fl[]=identifier&fl[]=title&fl[]=year&fl[]=licenseurl" +
            "&sort[]=downloads+desc&rows=$rows&page=1&output=json"
        val docs = JSONObject(get(url)).getJSONObject("response").getJSONArray("docs")
        (0 until docs.length()).mapNotNull { i ->
            val d = docs.getJSONObject(i)
            val license = d.optString("licenseurl")
            if (!isOpenLicense(license)) return@mapNotNull null
            Title(
                id = d.getString("identifier"),
                name = d.optString("title", d.getString("identifier")),
                year = d.optString("year", ""),
                license = license,
            )
        }
    }

    suspend fun detail(title: Title): TitleDetail = withContext(Dispatchers.IO) {
        val root = JSONObject(get("https://archive.org/metadata/${title.id}"))
        val meta = root.optJSONObject("metadata") ?: JSONObject()
        val license = meta.optString("licenseurl", title.license)
        val files = root.optJSONArray("files")
        val episodes = mutableListOf<Episode>()
        if (isOpenLicense(license) && files != null) {
            // Preferimos una sola versión MP4 de cada video.
            val mp4 = (0 until files.length()).map { files.getJSONObject(it) }
                .filter { it.optString("name").endsWith(".mp4", ignoreCase = true) }
            val preferred = mp4.filter {
                val f = it.optString("format").lowercase()
                f.contains("h.264") || f.contains("512kb")
            }.ifEmpty { mp4 }
            val seen = HashSet<String>()
            for (f in preferred.sortedBy { it.optString("name") }) {
                val name = f.optString("name")
                val base = name.substringBeforeLast('.').removeSuffix("_512kb")
                if (!seen.add(base)) continue
                episodes += Episode(
                    label = f.optString("title").ifBlank { base.replace('_', ' ') },
                    url = "https://archive.org/download/${title.id}/" + enc(name).replace("+", "%20"),
                )
            }
        }
        TitleDetail(
            title = title.copy(license = license),
            description = meta.optString("description").replace(Regex("<[^>]*>"), " ").trim(),
            episodes = episodes,
        )
    }

    fun isOpenLicense(url: String): Boolean =
        url.contains("publicdomain", true) || url.contains("creativecommons", true)

    fun licenseLabel(url: String): String = when {
        url.contains("publicdomain", true) -> "Dominio público"
        url.contains("creativecommons", true) -> "Creative Commons"
        else -> "Licencia desconocida"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "PantallaAbierta/1.0 (Android)")
        return try {
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
