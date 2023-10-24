package B_CSV

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import java.io.File
import java.net.URL
import kotlin.math.sqrt

/*
Pick your own file from https://waterinfo.rws.nl/#/publiek/golfhoogte
 */

fun main() {
    application {

        configure {
            width = 512
            height = 720
        }

        program {
            val csv = File("data/csv/Golfhoogte in cm Platform HKZA.csv").readText()

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
            extend(Camera2D())
            extend {
                val columns = 30

                drawer.circles {
                    for ((index, entry) in entries.withIndex()) {
                        val x = index % columns
                        val y = index / columns
                        fill = ColorRGBa.RED.toHSVa().shiftHue(entry.height).toRGBa()
                        circle(x * 20.0, y * 20.0, 0.1 * entry.height)
                    }
                }
            }
        }
    }
}