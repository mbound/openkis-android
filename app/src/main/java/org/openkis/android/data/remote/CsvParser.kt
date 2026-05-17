package org.openkis.android.data.remote

/**
 * Minimal RFC4180 CSV parser for the dev.catastogrotte-piemonte.net export format.
 *
 * The export CSVs have an unusual structure:
 *   Row 0 — numeric column indices (0,1,2,…N)  ← skipped
 *   Row 1 — column header names                ← used as map keys
 *   Row 2+ — data rows
 *
 * UTF-8 BOM is stripped if present.
 */
object CsvParser {

    fun parse(raw: String): List<Map<String, String>> {
        val content = raw.removePrefix("﻿")
        val rows = parseRows(content)

        // The first row whose first cell is not a plain integer is the header row.
        // (Row 0 is always "0,1,2,…" — we skip it.)
        val headerIdx = rows.indexOfFirst { row ->
            row.isNotEmpty() && row[0].trim().toIntOrNull() == null
        }
        if (headerIdx < 0) return emptyList()

        val headers = rows[headerIdx].map { it.trim() }
        return rows.drop(headerIdx + 1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                buildMap {
                    headers.forEachIndexed { i, header ->
                        put(header, if (i < row.size) row[i] else "")
                    }
                }
            }
    }

    private enum class State { FIELD_START, IN_FIELD, IN_QUOTED, AFTER_QUOTE }

    private fun parseRows(content: String): List<List<String>> {
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
            if (currentRow.isNotEmpty()) rows.add(currentRow.toList())
            currentRow.clear()
        }

        while (i < content.length) {
            val ch = content[i]
            when (state) {
                State.FIELD_START -> when (ch) {
                    '"' -> { state = State.IN_QUOTED; i++ }
                    ',' -> { emitField(); i++ }
                    '\r' -> { val next = content.getOrNull(i + 1); emitRow(); i += if (next == '\n') 2 else 1 }
                    '\n' -> { emitRow(); i++ }
                    else -> { currentField.append(ch); state = State.IN_FIELD; i++ }
                }
                State.IN_FIELD -> when (ch) {
                    ',' -> { emitField(); state = State.FIELD_START; i++ }
                    '\r' -> { val next = content.getOrNull(i + 1); emitRow(); state = State.FIELD_START; i += if (next == '\n') 2 else 1 }
                    '\n' -> { emitRow(); state = State.FIELD_START; i++ }
                    else -> { currentField.append(ch); i++ }
                }
                State.IN_QUOTED -> when (ch) {
                    '"' -> { state = State.AFTER_QUOTE; i++ }
                    else -> { currentField.append(ch); i++ }
                }
                State.AFTER_QUOTE -> when (ch) {
                    '"' -> { currentField.append('"'); state = State.IN_QUOTED; i++ }
                    ',' -> { emitField(); state = State.FIELD_START; i++ }
                    '\r' -> { val next = content.getOrNull(i + 1); emitRow(); state = State.FIELD_START; i += if (next == '\n') 2 else 1 }
                    '\n' -> { emitRow(); state = State.FIELD_START; i++ }
                    else -> { currentField.append(ch); state = State.IN_FIELD; i++ }
                }
            }
        }
        // Flush any trailing content
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) emitRow()
        return rows
    }
}
