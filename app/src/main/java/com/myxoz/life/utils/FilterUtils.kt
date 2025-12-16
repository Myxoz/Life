package com.myxoz.life.utils

import java.text.Normalizer

private val diacriticsRegex = "\\p{Mn}+".toRegex()

private fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(diacriticsRegex, "")
        .lowercase()

private fun matchScore(text: String, queryNorm: String, queryRaw: String): Int {
    val norm = text.normalizeForSearch()
    val wordsNorm = norm.split(' ')
    val wordsRaw = text.split(' ')
    if (norm == queryNorm) return 0
    for ((wRaw, wNorm) in wordsRaw.zip(wordsNorm)) {
        if (wNorm == queryNorm &&
            wRaw.length == queryRaw.length &&
            wRaw.equals(queryRaw, true)
        ) return 1
    }
    if (wordsNorm.any { it.startsWith(queryNorm, true) }) return 2
    if (norm.contains(queryNorm)) return 3
    return Int.MAX_VALUE
}

fun <T> List<T>.filteredWith(
    query: String,
    keyB: (T) -> String,
    keyA: (T) -> String
): List<T> {
    if (query.isBlank()) return this

    val qNorm = query.normalizeForSearch()

    return this
        .mapNotNull { item ->
            val a = keyA(item)
            val b = keyB(item)

            val scoreA = matchScore(a, qNorm, query)
            val scoreB = matchScore(b, qNorm, query)
            if (scoreA == Int.MAX_VALUE && scoreB == Int.MAX_VALUE) return@mapNotNull null
            val (fieldRank, score, primaryText) =
                if (scoreA <= scoreB) {
                    Triple(0, scoreA, a)
                } else {
                    Triple(1, scoreB, b)
                }

            Triple(item, fieldRank, Pair(score, primaryText.normalizeForSearch()))
        }
        .sortedWith(
            compareBy<Triple<T, Int, Pair<Int, String>>> { it.second }
                .thenBy { it.third.first }
                .thenBy { it.third.second }
        )
        .map { it.first }
}
