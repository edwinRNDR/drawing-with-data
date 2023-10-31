package E_CameraSound

import org.openrndr.application
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.math.Vector2
import org.openrndr.openal.AudioData
import org.openrndr.openal.AudioFormat
import org.openrndr.openal.AudioSystem
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main() {
    application {

        program {
            val intensities = DoubleArray(44100/30) { 0.0 }
            val aqs = AudioSystem.createQueueSource(2, 2) {
                val bb = ByteBuffer.allocateDirect(2 * 44100 / 30)
                bb.order(ByteOrder.nativeOrder())

                synchronized(intensities) {
                    println(intensities[0])
                    for (i in 0 until 44100 / 30) {
                        val v = intensities[i]
                        bb.putShort(((v-0.5) * 62767).toInt().toShort())
                    }
                }
                bb.rewind()
                AudioData(AudioFormat.MONO_16, 44100, bb)
            }
            aqs.play()
            VideoPlayerFFMPEG.listDeviceNames().forEach {
                println(it)
            }
            val camera = loadVideoDevice(deviceName = "Integrated Webcam", width = 640, height = 480)

            val rt = renderTarget(256, 256) {
                colorBuffer()
            }
            camera.play()
            val randoms = List(256*256) { it }.shuffled().take(44100/30).sorted()
            val points = randoms.map {
                Vector2(it.mod(256).toDouble(), (it/256).toDouble())
            }

            extend {
                val cb = camera.colorBuffer
                camera.draw(drawer, blind = true)
                if (cb != null) {
                    drawer.isolatedWithTarget(rt) {
                        drawer.ortho(rt)
                        drawer.imageFit(cb, drawer.bounds)
                    }
                    val s = rt.colorBuffer(0).shadow
                    s.download()
                    synchronized(intensities) {
                        for ((index, r) in randoms.withIndex()) {
                            val c = s[r.mod(256), r / 256]
                            intensities[index] = c.luminance
                        }
                    }
                    drawer.image(rt.colorBuffer(0))
                    drawer.circles(points, intensities.map { it * 4.0 })
                }
            }
        }
    }
}