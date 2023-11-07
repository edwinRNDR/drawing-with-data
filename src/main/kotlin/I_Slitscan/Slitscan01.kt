package I_Slitscan

import org.openrndr.application
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.Rectangle

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

            val c = compose {

                layer {
                    var offset = 0.0
                    var velocity = 1.0
                    clearColor = null
                    draw {
                        val lc = camera.colorBuffer
                        if (lc != null) {
                            drawer.image(
                                lc,
                                Rectangle(0.0, lc.height / 2.0, lc.width.toDouble(), 1.0),
                                Rectangle(0.0, offset, width.toDouble(), 1.0)
                            )
                            offset = (offset + velocity).mod(height.toDouble())
                        }
                    }
                }
            }

            extend {
                camera.draw(drawer, blind = true)
                c.draw(drawer)
            }
        }

    }
}