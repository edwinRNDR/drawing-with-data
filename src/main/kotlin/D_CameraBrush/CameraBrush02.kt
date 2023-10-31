package D_CameraBrush

import org.openrndr.application
import org.openrndr.extra.noclear.NoClear
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.Circle

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

            val points = drawer.bounds.scatter(100.0)
            val contours = points.map { Circle(it, 100.0).contour }

            camera.play()
            extend(NoClear())
            extend {
                camera.draw(drawer, blind = true)
                val cb = camera.colorBuffer

                val time = seconds
                val contourIndex = time.toInt().mod(contours.size)
                val contourT = time.mod(1.0)
                val s = 0.2
                val position = contours[contourIndex].position(contourT)
                if (cb != null) {


                    drawer.image(
                        cb,
                        position.x - cb.width * s * 0.5,
                        position.y - cb.height * s * 0.5,
                        cb.width * s,
                        cb.height * s
                    )
                }

            }
        }
    }
}