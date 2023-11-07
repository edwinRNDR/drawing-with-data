package I_Slitscan

import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 1280
            height = 720
            fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
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
                    val fitted = renderTarget(width, height, contentScale = 1.0) {
                        colorBuffer()
                    }
                    draw {
                        val lc = camera.colorBuffer
                        if (lc != null) {
                            drawer.isolatedWithTarget(fitted) {
                                drawer.imageFit(lc, drawer.bounds)
                            }
                            val fi = fitted.colorBuffer(0)
                            drawer.image(
                                fi,
                                Rectangle(0.0, offset, width.toDouble(), velocity),
                                Rectangle(0.0, offset, width.toDouble(), velocity)
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