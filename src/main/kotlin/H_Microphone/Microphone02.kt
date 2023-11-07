package H_Microphone

import ddf.minim.Minim
import ddf.minim.analysis.FFT
import ddf.minim.analysis.HammingWindow
import ddf.minim.analysis.LanczosWindow
import ddf.minim.analysis.WindowFunction
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.toOKHSVa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.minim.MinimObject
import org.openrndr.extra.minim.minim
import org.openrndr.extra.noclear.NoClear
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


            val c = compose {
                layer {
                    clearColor =null
                    var offset = 0.0
                    var velocity = 1.0
                    draw {
                        drawer.stroke = null
                        drawer.rectangles {
                            val h = height / 200.0
                            for (i in 0 until 200) {
                                val v = (fft.getBand(i))
                                fill = ColorRGBa.BLUE.toOKHSVa().shiftHue(v * 180.0).toRGBa()
                                rectangle(offset, i * h, velocity, h)
                            }
                        }
                        offset = (offset + velocity).mod(width.toDouble())
                    }
                }
            }

            extend {
                fft.forward(lineIn.mix)
                c.draw(drawer)

            }
        }
    }
}