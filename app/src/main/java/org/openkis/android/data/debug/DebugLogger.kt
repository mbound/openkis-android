package org.openkis.android.data.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String
)

@Singleton
class DebugLogger @Inject constructor() {

    private val capacity = 500
    private val buffer = ArrayDeque<LogEntry>(capacity)

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun d(tag: String, message: String) = append("D", tag, message)
    fun i(tag: String, message: String) = append("I", tag, message)
    fun w(tag: String, message: String) = append("W", tag, message)
    fun e(tag: String, message: String) = append("E", tag, message)

    fun getContent(): String {
        val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return synchronized(buffer) {
            buffer.joinToString("\n") { e ->
                "${fmt.format(Date(e.timestamp))} ${e.level}/${e.tag}: ${e.message}"
            }
        }
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _entries.value = emptyList()
        }
    }

    private fun append(level: String, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        synchronized(buffer) {
            if (buffer.size >= capacity) buffer.removeFirst()
            buffer.add(entry)
            _entries.value = buffer.toList()
        }
        Log.println(
            when (level) { "E" -> Log.ERROR; "W" -> Log.WARN; "I" -> Log.INFO; else -> Log.DEBUG },
            tag, message
        )
    }
}
