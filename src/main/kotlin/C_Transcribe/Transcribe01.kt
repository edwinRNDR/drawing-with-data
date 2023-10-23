package C_Transcribe

import audio.AudioPlayer
import lib.readSRT
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle
import org.openrndr.writer
import java.io.File

// https://colab.research.google.com/github/ArthurFDLR/whisper-youtube/blob/main/whisper_youtube.ipynb
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            extend(AudioPlayer()) {
                audioFile = "data/audio/HAHZTDQOj2I.ogg"
            }
            val transcript = readSRT(File("data/transcription/HAHZTDQOj2I.srt"))
            extend {
                writer {
                    box = Rectangle(40.0, 40.0, width - 80.0, height - 80.0)
                    for (item in transcript) {

                        if (item.startTime <= seconds && item.endTime > seconds) {
                            drawer.fill = ColorRGBa.YELLOW
                        } else {
                            drawer.fill = ColorRGBa.GRAY
                        }

                        for (textMessage in item.texts) {
                            text(textMessage + " ")
                        }
                    }
                }
            }
        }
    }

}