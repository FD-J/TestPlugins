package com.example // You can change this to your name

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class AnimeJokerProvider : MainAPI() { 
    override var mainUrl = "https://animejoker.com"
    override var name = "AnimeJoker"
    override val supportedTypes = setOf(TvType.Anime)

    // 1. SEARCH FUNCTION
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.result-item").mapNotNull {
            val title = it.selectFirst("h2.title a")?.text() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("src")
            
            newAnimeSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = poster
            }
        }
    }

    // 2. LOAD FUNCTION (Show Page & Episode List)
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: "No Title"
        val poster = document.selectFirst("div.poster img")?.attr("src")
        val plot = document.selectFirst("div.wp-content p")?.text()

        val episodes = document.select("ul.episodios li").mapNotNull {
            val epName = it.selectFirst("div.episodiotitle a")?.text() ?: return@mapNotNull null
            val epHref = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val epNum = it.selectFirst("div.numerando")?.text()?.filter { char -> char.isDigit() }?.toIntOrNull()

            newEpisode(epHref) {
                this.name = epName
                this.episode = epNum
            }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime, episodes) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    // 3. LINKS FUNCTION (The Video Player)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        // This looks for common video player iframes
        document.select("iframe").map { it.attr("src") }.forEach { link ->
            loadExtractor(link, data, subtitleCallback, callback)
        }
        return true
    }
}
