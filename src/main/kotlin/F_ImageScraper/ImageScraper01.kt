package F_ImageScraper

import lib.scrapeImages
import org.openrndr.application
import org.openrndr.extra.imageFit.imageFit

fun main() {
    application {
        program {
            val images = scrapeImages("halloween")
            extend {
                val index = (seconds.toInt()).mod(images.size)
                drawer.imageFit(images[index].imageSmall, drawer.bounds)
            }
        }
    }
}