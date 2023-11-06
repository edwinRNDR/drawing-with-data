package G_ProjectionMapping

import lib.BrushMapper
import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.imageFit.imageFit

fun main() {
    application {
        program {
            val image = loadImage("data/images/cheeta.jpg")

            extend(BrushMapper())
            extend {
                drawer.imageFit(image, drawer.bounds)
            }
        }
    }
}