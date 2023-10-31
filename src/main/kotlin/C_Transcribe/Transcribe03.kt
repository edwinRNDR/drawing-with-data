package C_Transcribe

import audio.AudioPlayer
import lib.readSRT
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.distort.StretchWaves
import org.openrndr.shape.Rectangle
import org.openrndr.writer
import java.io.File
import kotlin.math.cos

// https://colab.research.google.com/github/ArthurFDLR/whisper-youtube/blob/main/whisper_youtube.ipynb
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            extend(AudioPlayer()) {
                audioFile = "data/audio/FAbsoxQtUwM.ogg"

            }
            val transcript = readSRT(File("data/transcription/FAbsoxQtUwM.srt"))

            val c = compose {
                layer {
                    draw {
                        val time = seconds
                        val active =  transcript.filter {
                            it.startTime <= time && it.endTime > time
                        }
                        drawer.fontMap = loadFont("data/fonts/default.otf", 64.0)
                        writer {

                            box = Rectangle(40.0, 40.0, width - 80.0, height - 80.0)
                            newLine()
                            for (item in active) {
                                for (textMessage in item.texts) {
                                    text(textMessage)
                                }
                            }
                        }
                    }
                    post(StretchWaves()) {
                        phase = seconds
                        this.rotation = seconds * 10.0
                        this.distortion = 0.25
                        this.distortion = cos(seconds)

                    }
                    post(FrameBlur()) {
                        blend = 0.2
                    }

                }
            }

            extend {
                c.draw(drawer)
            }
        }
    }

}