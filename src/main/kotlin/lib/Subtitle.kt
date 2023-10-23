package lib

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import java.io.File

class Subtitle(val startTime: Double, val endTime: Double, val texts: List<String>)


enum class ExpectedInput {
    NUMBER,
    TIME,
    TEXT
}

fun readSRT(file: File): List<Subtitle> {
    val result = mutableListOf<Subtitle>()
    var index = 1
    var expected = ExpectedInput.NUMBER


    fun toSeconds(text: String): Double {
        val tokens = text.split(Regex("[:,]")).reversed().map {
            it.toInt()
        }
        return tokens[0] * 1.0 / 1000 + tokens[1] + tokens[2] * 60.0 + tokens[3] * 60.0 * 60.0
    }

    var cursorStart = -1.0
    var cursorEnd = -1.0

    var textBuffer = mutableListOf<String>()
    file.forEachLine {
        if (expected == ExpectedInput.NUMBER) {
            val nr = it.trim().toInt()
            if (nr != index) {
                throw RuntimeException("there is a problem")
            } else {
                expected = ExpectedInput.TIME
            }
        } else if (expected == ExpectedInput.TIME) {
            val tokens = it.split("-->").map { it.trim() }
            if (tokens.size != 2) {
                throw RuntimeException("expected 2 tokens")
            } else {
                cursorStart = toSeconds(tokens[0])
                cursorEnd = toSeconds(tokens[1])
                expected = ExpectedInput.TEXT
            }
        } else if (expected == ExpectedInput.TEXT) {
            if (it.trim().isNotEmpty()) {
                textBuffer.add(it.replace(Regex("<[/]?[a-z]+>"),""))
            } else {
                val subtitle = Subtitle(cursorStart, cursorEnd, textBuffer.map { it })
                result.add(subtitle)
                textBuffer.clear()
                expected = ExpectedInput.NUMBER
                index++
            }
        }
    }
    println("read ${result.size} subtitles")
    return result
}
