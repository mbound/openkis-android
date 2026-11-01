package org.openkis.android.data.remote

/**
 * Small RFC4180-style CSV parser used by the Piemonte export backend.
 *
 * The backend has existed in more than one export shape, so this parser accepts:
 * - comma, semicolon, or tab delimiters (auto-detected)
 * - an optional Excel-style "sep=;" first line
 * - the older numeric-index row ("0,1,2,…") before the real header
 * - UTF-8 BOM
 */
object CsvParser {

    data class ParseResult(
        val delimiter: Char,
        val headers: List<String>,
        val rows: List<Map<String, String>>
    )

    fun parse(raw: String): List<Map<String, String>> = parseDetailed(raw).rows

    fun parseDetailed(raw: String): ParseResult {
        var content = raw.removePrefix("\uFEFF")
        val explicitDelimiter = content.lineSequence()
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.length == 5 && it.startsWith("sep=", ignoreCase = true) }
            ?.last()

        if (explicitDelimiter != null) {
            content = content.substringAfter('\n', "")
        }

        val delimiter = explicitDelimiter ?: detectDelimiter(content)
        val rows = parseRows(content, delimiter)

        val headerIdx = rows.indexOfFirst { row -> isLikelyHeader(row) }
        if (headerIdx < 0) {
            return ParseResult(delimiter, emptyList(), emptyList())
        }

        val headers = rows[headerIdx].mapIndexed { index, value ->
            value.removePrefix("\uFEFF").trim().ifBlank { "column_$index" }
        }

        val mapped = rows.drop(headerIdx + 1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                buildMap {
                    headers.forEachIndexed { i, header ->
                        put(header, if (i < row.size) row[i].trim() else "")
                    }
                }
            }

        return ParseResult(delimiter, headers, mapped)
    }

    private fun isLikelyHeader(row: List<String>): Boolean {
        if (row.size < 2) return false

        // Old exporter emits a row containing only numeric column indexes before
        // the real header.
        if (row.all { it.trim().toIntOrNull() != null }) return false

        val normalized = row.map(::normalizeHeader)
        val known = setOf(
            "codice", "code", "nome", "name", "id",
            "latitudine", "latitude", "lat",
            "longitudine", "longitude", "lon", "lng",
            "quota", "altitudine", "elevation"
        )

        return normalized.any { it in known } ||
            // Fallback for future schemas: the first non-numeric multi-column row.
            row.count { it.isNotBlank() } >= 2
    }

    private fun normalizeHeader(value: String): String =
        value.trim()
            .lowercase()
            .replace('à', 'a')
            .replace('á', 'a')
            .replace('è', 'e')
            .replace('é', 'e')
            .replace('ì', 'i')
            .replace('í', 'i')
            .replace('ò', 'o')
            .replace('ó', 'o')
            .replace('ù', 'u')
            .replace('ú', 'u')
            .filter { it.isLetterOrDigit() }

    private fun detectDelimiter(content: String): Char {
        val sampleLines = content.lineSequence()
            .filter { it.isNotBlank() }
            .take(5)
            .toList()

        if (sampleLines.isEmpty()) return ','

        return listOf(',', ';', '\t')
            .maxByOrNull { delimiter ->
                sampleLines.sumOf { countUnquoted(it, delimiter) }
            }
            ?: ','
    }

    private fun countUnquoted(line: String, delimiter: Char): Int {
        var quoted = false
        var count = 0
        var i = 0
        while (i < line.length) {
            when (line[i]) {
                '"' -> {
                    if (quoted && line.getOrNull(i + 1) == '"') {
                        i++
                    } else {
                        quoted = !quoted
                    }
                }
                delimiter -> if (!quoted) count++
            }
            i++
        }
        return count
    }

    private enum class State { FIELD_START, IN_FIELD, IN_QUOTED, AFTER_QUOTE }

    private fun parseRows(content: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var state = State.FIELD_START
        var i = 0

        fun emitField() {
            currentRow.add(currentField.toString())
            currentField.clear()
        }

        fun emitRow() {
            emitField()
            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
            currentRow.clear()
        }

        while (i < content.length) {
            val ch = content[i]
            when (state) {
                State.FIELD_START -> when (ch) {
                    '"' -> {
                        state = State.IN_QUOTED
                        i++
                    }
                    delimiter -> {
                        emitField()
                        i++
                    }
                    '\r' -> {
                        val next = content.getOrNull(i + 1)
                        emitRow()
                        i += if (next == '\n') 2 else 1
                    }
                    '\n' -> {
                        emitRow()
                        i++
                    }
                    else -> {
                        currentField.append(ch)
                        state = State.IN_FIELD
                        i++
                    }
                }

                State.IN_FIELD -> when (ch) {
                    delimiter -> {
                        emitField()
                        state = State.FIELD_START
                        i++
                    }
                    '\r' -> {
                        val next = content.getOrNull(i + 1)
                        emitRow()
                        state = State.FIELD_START
                        i += if (next == '\n') 2 else 1
                    }
                    '\n' -> {
                        emitRow()
                        state = State.FIELD_START
                        i++
                    }
                    else -> {
                        currentField.append(ch)
                        i++
                    }
                }

                State.IN_QUOTED -> when (ch) {
                    '"' -> {
                        state = State.AFTER_QUOTE
                        i++
                    }
                    else -> {
                        currentField.append(ch)
                        i++
                    }
                }

                State.AFTER_QUOTE -> when (ch) {
                    '"' -> {
                        currentField.append('"')
                        state = State.IN_QUOTED
                        i++
                    }
                    delimiter -> {
                        emitField()
                        state = State.FIELD_START
                        i++
                    }
                    '\r' -> {
                        val next = content.getOrNull(i + 1)
                        emitRow()
                        state = State.FIELD_START
                        i += if (next == '\n') 2 else 1
                    }
                    '\n' -> {
                        emitRow()
                        state = State.FIELD_START
                        i++
                    }
                    else -> {
                        // Be permissive about whitespace or other characters after
                        // a closing quote.
                        currentField.append(ch)
                        state = State.IN_FIELD
                        i++
                    }
                }
            }
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) emitRow()
        return rows
    }
}
