package com.ghostreborn.akira.features.manga.models

/**
 * Represents a single page image within a manga chapter.
 */
data class MangaPage(
    val pageNumber: Int,
    val imageUrl: String,
    val headers: Map<String, String> = mapOf(
        "Referer" to "https://mkissa.to/",
        "Origin" to "https://mkissa.to"
    )
)

/**
 * Reading display mode: Webtoon (vertical infinite scroll) or Paged (flip).
 */
enum class ReadingMode {
    WEBTOON,
    PAGED_LTR,
    PAGED_RTL
}

/**
 * Reading progress state for tracking last read chapter and page.
 */
data class MangaReadingProgress(
    val mangaId: String,
    val lastChapterString: String,
    val lastPageNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)
