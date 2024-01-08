package J_Midi

import org.openrndr.application
import org.openrndr.extra.midi.openMidiDevice

fun main() {
    application {
        program {

            val device = openMidiDevice("M3000")

            var radius = 0.0
            device.controlChanged.listen {
                if (it.channel == 11) {
                    radius = it.value.toDouble()
                }
            }

            extend {
                drawer.circle(width / 2.0, height / 2.0, radius)
            }
        }
    }
}