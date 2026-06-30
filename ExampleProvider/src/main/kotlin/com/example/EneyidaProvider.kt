package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class EneyidaProvider : MainAPI() {
    override var mainUrl = "https://eneyida.tv"
    override var name = "Eneyida"
    override val hasMainPage = true
    override var lang = "uk"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.Cartoon
    )

    // --- 1. ГОЛОВНА СТОРІНКА ---
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Формуємо URL залежно від сторінки
        val url = if (page == 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(url).document
        
        // Знаходимо картки релізів. Тег 'article.short' є типовим для Eneyida
        val items = document.select("article.short").mapNotNull {
            it.toSearchResult()
        }
        
        return newHomePageResponse("Останні надходження", items)
    }

    // --- 2. ПОШУК ---
    override suspend fun search(query: String): List<SearchResponse> {
        // Eneyida часто використовує POST-запити для пошуку, але GET через /search/ також може працювати.
        // Застосовуємо GET-запит через URL кодування:
        val url = "$mainUrl/search/${query.replace(" ", "+")}/"
        val document = app.get(url).document
        
        return document.select("article.short").mapNotNull {
            it.toSearchResult()
        }
    }

    // --- 3. СТОРІНКА ФІЛЬМУ / СЕРІАЛУ ---
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        // Витягуємо метадані
        val title = document.selectFirst("h1[itemprop=name]")?.text() ?: ""
        val poster = document.selectFirst("div.full_poster img")?.attr("src")?.let { fixUrl(it) }
        val description = document.selectFirst("div.full_text")?.text() ?: ""
        
        // Витягуємо інформацію для серіалів (якщо є) або створюємо як фільм
        // Наразі повертаємо базову структуру Movie для спрощення
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    // --- 4. ЕКСТРАКЦІЯ ВІДЕО ---
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Шукаємо всі iframe на сторінці (плеєри Ashdi, Tortuga тощо)
        val iframes = document.select("iframe")
        
        iframes.forEach { iframe ->
            val iframeUrl = iframe.attr("src")
            if (iframeUrl.isNotBlank()) {
                // Використовуємо вбудовану систему Cloudstream для парсингу відомих плеєрів
                val fixedUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl
                loadExtractor(fixedUrl, data, subtitleCallback, callback)
            }
        }
        return true
    }

    // --- ДОПОМІЖНІ ФУНКЦІЇ ---
    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("div.short_title a") ?: return null
        val title = titleElement.text()
        val href = titleElement.attr("href")
        val poster = this.selectFirst("div.short_img img")?.attr("src")?.let { fixUrl(it) }
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }
}
