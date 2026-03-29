package com.example.photoroulette.utils

import kotlin.math.max

object VersionNameUtils {

    fun normalize(versionName: String): String {
        return versionName
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .ifBlank { "0" }
    }

    fun isNewer(remoteVersion: String, localVersion: String): Boolean {
        return compare(remoteVersion, localVersion) > 0
    }

    fun compare(firstVersion: String, secondVersion: String): Int {
        val firstParts = splitToNumericParts(normalize(firstVersion))
        val secondParts = splitToNumericParts(normalize(secondVersion))
        val partCount = max(firstParts.size, secondParts.size)

        for (index in 0 until partCount) {
            val firstPart = firstParts.getOrElse(index) { 0 }
            val secondPart = secondParts.getOrElse(index) { 0 }
            if (firstPart != secondPart) {
                return firstPart.compareTo(secondPart)
            }
        }

        return 0
    }

    private fun splitToNumericParts(versionName: String): List<Int> {
        val numericParts = versionName
            .split('.', '-', '_')
            .map { token ->
                token
                    .takeWhile { char -> char.isDigit() }
                    .toIntOrNull()
                    ?: 0
            }

        return if (numericParts.isEmpty()) {
            listOf(0)
        } else {
            numericParts
        }
    }
}
