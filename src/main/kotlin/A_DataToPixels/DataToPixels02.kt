package A_DataToPixels

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.yield
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.spaces.toOKHSVa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.color.Duotone
import org.openrndr.extra.fx.color.DuotoneGradient
import org.openrndr.launch
import java.io.File
import kotlin.math.floor

fun main() = application {
    configure {
        width = 512
        height = 720
    }
    program {
        var loadJob: Job? = null
        val flipChannel = Channel<Unit>()
        val image = colorBuffer(width, height)

        var sourceFile = ""

        keyboard.character.listen {
            if (it.character == 'n') {
                flipChannel.trySend(Unit)
            }
        }

        window.drop.listen {

            sourceFile = File(it.files.first()).name

            loadJob?.cancel()
            loadJob = launch {
                val file = File(it.files.first())
                val reader = file.inputStream()
                val data = ByteArray(width * height)

                while (true) {
                    data.fill(0)
                    val read = reader.read(data)
                    val s = image.shadow
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val v = data[y * width + x] / 255.0
                            s[x, y] = ColorRGBa.RED.toOKHSVa().shiftHue(v * 30.0).toRGBa().toSRGB()
                        }
                    }
                    s.upload()
                    flipChannel.receive()
                }
            }
        }

        val c = compose {
            layer {
                draw {
                    image.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
                    drawer.image(image)
                }
//                post(FrameBlur()) {
//                    blend = 0.5
//                }
            }
        }
        extend(Screenshots())
        extend(Camera2D())
        extend {
            c.draw(drawer)
            drawer.defaults()
            drawer.text(sourceFile, 10.0, height - 10.0)
        }
    }
}