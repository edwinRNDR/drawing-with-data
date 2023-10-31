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
            val csv = File("data/csv/Watertemperatuur in oC Platform HKZA.csv").readText()

            data class Entry(val date: String, val time: String, val location: String, val height: Double)
            val entries = csvReader {
                delimiter = ';'
            }.readAllWithHeader(csv).map {
                Entry(
                    it["Datum"]!!,
                    it["Tijd (CEST)"]!!,
                    it["Locatie"]!!,
                    it["Watertemperatuur in oC"]?.replace(",",".")?.toDoubleOrNull() ?: -1.0
                )
            }
            extend(Camera2D())
            extend {
                val columns = 30

                for (entry in entries.take(400)) {
                    drawer.text(entry.date, entry.height * 10.0, 0.0)
                    drawer.text(entry.time, 100.0 + entry.height * 10.0, 0.0)
                    drawer.text(entry.height.toString(), 200.0 + entry.height * 10.0, 0.0)
                    drawer.translate(0.0, 15.0)
                }
            }
        }
    }
}