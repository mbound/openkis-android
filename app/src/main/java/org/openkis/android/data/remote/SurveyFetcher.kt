package org.openkis.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openkis.android.data.debug.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

data class SurveyData(
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val date: String,
    val author: String,
    val surveyors: String,
    val speleoGroups: String,
    val license: String,
    val description: String,
    val bibliography: String
)

@Singleton
class SurveyFetcher @Inject constructor(
    private val client: OkHttpClient,
    private val logger: DebugLogger
) {
    suspend fun fetchSurveys(
        serverUrl: String,
        entityType: String,
        dbId: String
    ): List<SurveyData> = withContext(Dispatchers.IO) {
        val url = "${serverUrl.trimEnd('/')}/${entityType}-view-${dbId}.html"
        logger.d("SurveyFetcher", "Fetching surveys from $url")

        val html = try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.w("SurveyFetcher", "HTTP ${response.code} for $url")
                    return@withContext emptyList()
                }
                response.body?.string() ?: return@withContext emptyList()
            }
        } catch (e: Exception) {
            logger.e("SurveyFetcher", "Fetch failed: ${e.message}")
            throw e
        }

        parseSurveys(html, serverUrl)
    }

    suspend fun downloadImage(url: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.w("SurveyFetcher", "Image download HTTP ${response.code}: $url")
                    return@use false
                }
                destFile.parentFile?.mkdirs()
                response.body?.byteStream()?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        } catch (e: Exception) {
            logger.e("SurveyFetcher", "Image download error: ${e.message}")
            false
        }
    }

    private fun parseSurveys(html: String, serverUrl: String): List<SurveyData> {
        val doc = Jsoup.parse(html)

        // Find the <h3> that starts the surveys section (text contains "Rilievi")
        val rilievi = doc.select("h3").firstOrNull { h3 ->
            h3.text().contains("Rilievi", ignoreCase = true)
        } ?: return emptyList()

        // Collect the survey elements: the starting h3 and all its following siblings
        // until we hit a structural boundary (button, script, non-survey content).
        // Each <h3> (including the first) starts a new survey block.
        val parent = rilievi.parent() ?: return emptyList()
        val children = parent.children()
        val startIdx = children.indexOf(rilievi)
        if (startIdx < 0) return emptyList()

        // Group elements by h3 boundaries
        val groups = mutableListOf<MutableList<Element>>()
        var current: MutableList<Element>? = null

        for (i in startIdx until children.size) {
            val el = children[i]
            when {
                el.tagName() == "h3" -> {
                    // A new h3 starts a new survey block
                    current = mutableListOf(el)
                    groups.add(current)
                }
                // Stop at structural boundaries outside the survey section
                el.tagName() == "br" && current == null -> continue
                el.tagName() == "script" -> break
                el.hasClass("btn") || (el.tagName() == "a" && el.hasClass("btn")) -> break
                else -> current?.add(el)
            }
        }

        return groups.mapNotNull { group -> parseSurveyBlock(group, serverUrl) }
    }

    private fun parseSurveyBlock(
        elements: List<Element>,
        serverUrl: String
    ): SurveyData? {
        var title = ""
        var imageUrl = ""
        var thumbnailUrl = ""
        val fields = mutableMapOf<String, String>()

        for (el in elements) {
            when {
                el.tagName() == "h4" -> title = el.text().trim()

                el.hasClass("text-center") -> {
                    val link = el.selectFirst("a[href]")
                    val img = el.selectFirst("img[src]")
                    val href = link?.attr("href") ?: ""
                    val src = img?.attr("src") ?: ""
                    if (href.isNotBlank()) imageUrl = href
                    if (src.isNotBlank()) thumbnailUrl = src
                }

                el.hasClass("row") -> {
                    val labelEl = el.selectFirst(".text-info")
                    val valueEl = labelEl?.nextElementSibling()
                    val label = labelEl?.text()?.trim() ?: continue
                    val value = valueEl?.text()?.trim() ?: ""
                    if (value.isNotBlank()) fields[label] = value
                }
            }
        }

        // If there's no title or image this isn't a real survey block
        if (title.isBlank() && imageUrl.isBlank()) return null

        return SurveyData(
            title = title,
            imageUrl = resolveUrl(imageUrl, serverUrl),
            thumbnailUrl = resolveUrl(thumbnailUrl, serverUrl),
            date = fields["Data rilievo"] ?: "",
            author = fields["Autore rilievo"] ?: fields["Autore"] ?: "",
            surveyors = fields["Rilevatori"] ?: "",
            speleoGroups = fields["Gruppi speleo"] ?: "",
            license = fields["Licenza rilievo"] ?: fields["Licenza d'uso"] ?: "",
            description = fields["Descrizione rilievo"] ?: fields["Descrizione"] ?: "",
            bibliography = fields["Fonte bibliografica"] ?: ""
        )
    }

    private fun resolveUrl(url: String, serverUrl: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return "${serverUrl.trimEnd('/')}/$url"
    }
}
