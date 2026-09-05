package com.example.fakeloc

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

object AppLogger {
    private const val MAX_LINES = 500
    private const val FILE_NAME = "fakeloc.log"
    private const val TAG = "FakeLoc"

    private val buffer = ConcurrentLinkedDeque<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var file: File? = null

    fun init(context: Context) {
        file = File(context.filesDir, FILE_NAME)
    }

    fun d(msg: String)  { log("DEBUG", msg) }
    fun i(msg: String)  { log("INFO ", msg) }
    fun w(msg: String, t: Throwable? = null) { log("WARN ", msg, t) }
    fun e(msg: String, t: Throwable? = null) { log("ERROR", msg, t) }

    fun snapshot(): List<String> = buffer.toList()
    fun clear() = buffer.clear()

    fun logFile(): File? = file

    fun copyToString(): String = buffer.joinToString("\n")

    private fun log(level: String, msg: String, t: Throwable? = null) {
        val ts = fmt.format(Date())
        val line = "$ts $level $msg" + (t?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: "")
        buffer.addLast(line)
        while (buffer.size > MAX_LINES) buffer.pollFirst()
        Log.println(
            when (level.trim()) {
                "ERROR" -> Log.ERROR
                "WARN"  -> Log.WARN
                "INFO"  -> Log.INFO
                else    -> Log.DEBUG
            },
            TAG, line
        )
        runCatching {
            file?.appendText(line + "\n")
        }
    }
}
