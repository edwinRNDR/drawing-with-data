package D_CameraBrush

import org.openrndr.application
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noclear.NoClear
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.Circle
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

            val rt = renderTarget(width, height) {
                colorBuffer()
            }
            var buttonDown = false
            mouse.buttonDown.listen {
                buttonDown = true
            }
            mouse.buttonUp.listen {
                buttonDown = false
            }

            val c = compose {
                layer {
                    clearColor = null
                    draw {
                        if (buttonDown) {
                            val r = Rectangle.fromCenter(mouse.position, 100.0, 100.0)
                            drawer.image(rt.colorBuffer(0), r, r)
                        }
                    }
                }
            }

            camera.play()
            extend {
                camera.draw(drawer, blind = true)
                val cb = camera.colorBuffer
                if (cb != null) {
                    drawer.isolatedWithTarget(rt) {
                        val r = Rectangle(width*1.0, 0.0, -width*1.0, height * 1.0)
                        drawer.imageFit(cb, r)
                    }
                }
                c.draw(drawer)
                val r = Rectangle.fromCenter(mouse.position, 100.0, 100.0)
                drawer.image(rt.colorBuffer(0), r, r)
            }
        }
    }
}