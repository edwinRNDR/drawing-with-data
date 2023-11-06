package D_CameraBrush

import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noclear.NoClear
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            VideoPlayerFFMPEG.listDeviceNames().forEach {
                println(it)
            }
            val camera = loadVideoDevice(deviceName = "Integrated Webcam", width = 640, height = 480)
            camera.play()
            extend(Screenshots())
            extend(NoClear())
            extend {
                camera.draw(drawer, blind = true)
                val cb = camera.colorBuffer

                val s = 0.2
                if (cb != null) {
                    drawer.image(
                        cb,
                        mouse.position.x - cb.width * s * 0.5,
                        mouse.position.y - cb.height * s * 0.5,
                        cb.width * s,
                        cb.height * s
                    )
                }

            }
        }
    }
}