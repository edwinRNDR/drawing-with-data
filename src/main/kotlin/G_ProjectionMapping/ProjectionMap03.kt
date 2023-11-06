package G_ProjectionMapping

import lib.BrushMapper
import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice

fun main() {
    application {
        program {
            VideoPlayerFFMPEG.listDeviceNames().forEach {
                println(it)
            }
            val camera = loadVideoDevice(deviceName = "Integrated Webcam", width = 640, height = 480)
            camera.play()
            val bm = extend(BrushMapper())
            extend {
                camera.draw(drawer, blind = true)
                val lc = camera.colorBuffer
                if (lc != null) {
                    drawer.imageFit(lc, drawer.bounds)
                }
                bm.uvmapper.uShift = seconds

            }
        }
    }
}