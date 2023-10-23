package A_DataToText

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.spaces.toOKHSVa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.launch
import org.openrndr.writer
import java.io.File

fun main() = application {
    configure {
        width = 512
        height = 720
    }
    program {
        var loadJob: Job? = null
        val flipChannel = Channel<Unit>()
        val image = colorBuffer(width, height)
        keyboard.character.listen {
            if (it.character == 'n') {
                flipChannel.trySend(Unit)
            }
        }

        val maxCharacters = 2500
        var text = ""

        window.drop.listen {
            loadJob?.cancel()
            loadJob = launch {
                val file = File(it.files.first())
                val reader = file.inputStream()
                val data = ByteArray(maxCharacters)

                while (true) {
                    data.fill(0)
                    val read = reader.read(data)
                    println(read)
                    text = String(data).windowed(80, 80,partialWindows = true).joinToString("\n")

                    flipChannel.receive()
                }
            }
        }

        val c = compose {
            layer {
                draw {
                    drawer.fontMap = loadFont("data/fonts/default.otf", 16.0)
                    (drawer.fontMap as FontImageMap).texture.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
                    drawer.fill = ColorRGBa.WHITE
                    writer {
                        box = drawer.bounds.offsetEdges(-20.0)
                        text(text)
                    }

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
        }
    }
}