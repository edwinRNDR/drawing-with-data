package B_CSV

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import java.net.URL
import kotlin.math.sqrt

/*
Pick your own file from
 */

fun main() {
    application {

        configure {
            width = 512
            height = 720
        }

        program {

            val url =
                "https://waterinfo.rws.nl/api/chart/get?mapType=golfhoogte&locationCode=Platform-HKZA(HKZA)-1&values=-672%2C0"
            val csv = URL(url).readText()

            data class Entry(val date: String, val time: String, val location: String, val height: Double)
            val entries = csvReader {
                delimiter = ';'
            }.readAllWithHeader(csv).map {
                Entry(
                    it["Datum"]!!,
                    it["Tijd (CEST)"]!!,
                    it["Locatie"]!!,
                    it["Golfhoogte in cm"]?.toDoubleOrNull() ?: -1.0
                )
            }
            println(entries)
            println(entries.size)
            extend(Camera2D())
            extend {
                val texts = mutableListOf<String>()
                val textPositions = mutableListOf<Vector2>()
                val circles = mutableListOf<Circle>()

                var y = 20.0
                for (entry in entries.reversed().take(6*24*4)) {
                    texts.add(entry.date)
                    textPositions.add(Vector2(0.0, y))
                    texts.add(entry.time)
                    textPositions.add(Vector2(100.0, y))
                    circles.add(Circle(200.0, y, 0.25 * (entry.height)))
                    y += 15.0
                }
                drawer.texts(texts, textPositions)
                drawer.fill = ColorRGBa.WHITE.opacify(0.5)
                drawer.circles(circles)

            }
        }
    }
}