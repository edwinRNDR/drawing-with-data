package H_Microphone

import ddf.minim.Minim
import ddf.minim.analysis.FFT
import ddf.minim.analysis.HammingWindow
import ddf.minim.analysis.LanczosWindow
import ddf.minim.analysis.WindowFunction
import org.openrndr.application
import org.openrndr.extra.minim.MinimObject
import org.openrndr.extra.minim.minim
import org.openrndr.math.map
import kotlin.math.ln

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }

        program {
            val minim = minim()
            if (minim.lineOut == null) {
                application.exit()
            }

            val lineIn = minim.getLineIn(Minim.MONO, 2048, 44100f)
            if (lineIn == null) {
                application.exit()
            }
            val fft = FFT(lineIn.bufferSize(), lineIn.sampleRate())
            fft.window(LanczosWindow())

            extend {
                fft.forward(lineIn.mix)
                drawer.rectangles {
                    for (i in 0 until 200) {
                        val v = (2.0 * fft.getBand(i))
                        rectangle(i * 5.0, height / 2.0, 5.0, v * -10.0)
                    }
                }
            }
        }
    }
}