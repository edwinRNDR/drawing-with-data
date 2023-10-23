/**
 * Be sure to add these in build.gradle.kts in dependencies { }
 *
 *     implementation("org.lwjgl:lwjgl-stb:3.3.2")
 *     implementation("org.lwjgl:lwjgl-openal:3.3.2")
 *
 */

package audio

import org.lwjgl.openal.*
import org.lwjgl.openal.AL10.AL_GAIN
import org.lwjgl.openal.AL10.AL_PITCH
import org.lwjgl.stb.STBVorbis
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.events.listen
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


interface ProgressUpdater {
    fun makeCurrent(current: Boolean)
    fun updateProgress()
}


object AudioSystem {
    val device: Long = ALC10.alcOpenDevice(null as ByteBuffer?)
    val context: Long = ALC10.alcCreateContext(device, null as IntBuffer?).also {
        EXTThreadLocalContext.alcSetThreadContext(it)
    }
    val deviceCaps: ALCCapabilities = ALC.createCapabilities(device)
    val caps = AL.createCapabilities(deviceCaps)
    fun destroy() {
        ALC10.alcDestroyContext(context)
        ALC10.alcCloseDevice(device)
    }
}

class VorbisTrack(filePath: String) {
    private var encodedAudio: ByteBuffer? = null
    private var handle: Long = 0
    var channels = 0
    var sampleRate = 0
    val samplesLength: Int
    val samplesSec: Float
    val sampleIndex: AtomicInteger

    private var audioRenderer: AudioRenderer? = null
    private var audioThread: Thread? = null

    private val playing = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    private val cuePoints: MutableMap<Double, Event<VorbisTrack>> = mutableMapOf()


    fun duration(): Double {
        return samplesSec.toDouble()
    }

    fun cue(time: Double): Event<VorbisTrack> {

        val e = Event<VorbisTrack>()
        if (time >= 0.0) {
            cuePoints[time] = e
        } else {
            cuePoints[duration() + time] = e
        }
        return e
    }

    var gain = 1.0
        set(value) {
            field = value
            if (audioRenderer != null) {
                EXTThreadLocalContext.alcSetThreadContext(AudioSystem.context)
            }
            audioRenderer?.gain(value)
        }
    var pitch = 1.0
        set(value) {
            field = value
            if (audioRenderer != null) {
                EXTThreadLocalContext.alcSetThreadContext(AudioSystem.context)
            }
            audioRenderer?.pitch(value)
        }


    val finished = Event<VorbisTrack>("finished")

    fun play(initialGain: Double? = null, loop: Boolean = false) {

        if (initialGain != null) {
            gain = initialGain
        }


        if (!playing.get()) {
            playing.set(true)
            stopRequested.set(false)
            if (audioThread == null) {
                audioThread = thread(isDaemon = true) {
                    audioRenderer = AudioRenderer(this)
                    val progressUpdater = audioRenderer!!.progressUpdater
                    if (!audioRenderer!!.play(gain)) {
                        error("krak")
                    }
                    while (!stopRequested.get()) {
                        val haveWork = audioRenderer!!.update(loop)
                        Thread.sleep(5)
                        progressUpdater.updateProgress()

                        val p = position()
                        val toTrigger = cuePoints.filterKeys { it <= p }
                        toTrigger.toList().sortedBy { it.first }.forEach {
                            it.second.trigger(this)
                        }
                        cuePoints.keys.removeIf { it <= p }
                        if (!haveWork) {
                            break
                        }

                    }
                    playing.set(false)

                    println("finished $this, triggering event")
                    finished.trigger(this)
                    audioRenderer?.destroy()
                }
            }
        }
    }

    fun stop() {
        println("requesting stop")
        if (playing.get()) {
            println("requested stop")
            stopRequested.set(true)
        }
    }


    private fun ioResourceToByteBuffer(filePath: String, blockSize: Int): ByteBuffer {
        val path = Paths.get(filePath)
        if (Files.isReadable(path)) {
            Files.newByteChannel(path).use { fc ->
                val buffer = ByteBuffer.allocateDirect(fc.size().toInt() + 1)
                buffer.order(ByteOrder.nativeOrder())
                while (fc.read(buffer) != -1) {
                }
                buffer.flip()
                return buffer
            }

        }
        error("could not load $filePath")
    }

    init {
        encodedAudio = ioResourceToByteBuffer(filePath, 256 * 1024)
        MemoryStack.stackPush().use { stack ->
            val error: IntBuffer = stack.mallocInt(1)
            handle = STBVorbis.stb_vorbis_open_memory(encodedAudio, error, null)
            if (handle == org.lwjgl.system.MemoryUtil.NULL) {
                throw RuntimeException("Failed to open Ogg Vorbis file. Error: " + error[0])
            }
            val info: STBVorbisInfo = STBVorbisInfo.malloc(stack)
            print(info)
            channels = info.channels()
            sampleRate = info.sample_rate()
        }
        samplesLength = STBVorbis.stb_vorbis_stream_length_in_samples(handle)
        samplesSec = STBVorbis.stb_vorbis_stream_length_in_seconds(handle)
        this.sampleIndex = AtomicInteger(0)
        sampleIndex.set(0)
    }

    fun destroy() {
        STBVorbis.stb_vorbis_close(handle)
    }

    fun progressBy(samples: Int) {
        sampleIndex.set(sampleIndex.get() + samples)
    }

    fun setSampleIndex(sampleIndex: Int) {
        this.sampleIndex.set(sampleIndex)
    }

    fun rewind() {
        seek(0)
    }

    fun skip(direction: Int) {
        seek(
            Math.min(
                Math.max(0, STBVorbis.stb_vorbis_get_sample_offset(handle) + direction * sampleRate),
                samplesLength
            )
        )
    }

    fun skipTo(offset0to1: Float) {
        seek(Math.round(samplesLength * offset0to1))
    }

    fun relativePosition(): Double {
        return sampleIndex.get().toDouble() / samplesLength
    }

    fun position(): Double {
        return sampleIndex.get().toDouble() / sampleRate
    }

    // called from audio thread
    @Synchronized
    fun getSamples(pcm: ShortBuffer?): Int {
        return STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, pcm)
    }

    // called from UI thread
    @Synchronized
    private fun seek(sampleIndex: Int) {
        STBVorbis.stb_vorbis_seek(handle, sampleIndex)
        setSampleIndex(sampleIndex)
    }

    private fun print(info: STBVorbisInfo) {
//        println("stream length, samples: " + STBVorbis.stb_vorbis_stream_length_in_samples(handle))
//        println("stream length, seconds: " + STBVorbis.stb_vorbis_stream_length_in_seconds(handle))
//        println()
        STBVorbis.stb_vorbis_get_info(handle, info)
//        println("channels = " + info.channels())
//        println("sampleRate = " + info.sample_rate())
//        println("maxFrameSize = " + info.max_frame_size())
//        println("setupMemoryRequired = " + info.setup_memory_required())
//        println("setupTempMemoryRequired() = " + info.setup_temp_memory_required())
//        println("tempMemoryRequired = " + info.temp_memory_required())
    }
}

class AudioRenderer internal constructor(private val track: VorbisTrack) {
    private var format: Int = 0

    private val source: Int
    private val buffers: IntBuffer
    private val pcm: ShortBuffer
    var bufferOffset: Long = 0 // offset of last processed buffer
    var offset: Long = 0 // bufferOffset + offset of current buffer
    var lastOffset: Long = 0 // last offset update

    init {
        when (track.channels) {
            1 -> format = AL10.AL_FORMAT_MONO16
            2 -> format = AL10.AL_FORMAT_STEREO16
            else -> throw UnsupportedOperationException("Unsupported number of channels: " + track.channels)
        }
//        device = ALC10.alcOpenDevice(null as ByteBuffer?)
//        if (device == MemoryUtil.NULL) {
//            throw IllegalStateException("Failed to open the default device.")
//        }
//        context = ALC10.alcCreateContext(device, null as IntBuffer?)
//        if (context == MemoryUtil.NULL) {
//            throw IllegalStateException("Failed to create an OpenAL context.")
//        }


        pcm = MemoryUtil.memAllocShort(BUFFER_SIZE)
        EXTThreadLocalContext.alcSetThreadContext(AudioSystem.context)

        source = AL10.alGenSources()
        AL10.alSourcei(source, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL10.AL_TRUE)
        buffers = MemoryUtil.memAllocInt(2)
        AL10.alGenBuffers(buffers)
    }

    fun destroy() {
        AL10.alDeleteBuffers(buffers)
        AL10.alDeleteSources(source)
        MemoryUtil.memFree(buffers)
        MemoryUtil.memFree(pcm)
        EXTThreadLocalContext.alcSetThreadContext(MemoryUtil.NULL)

    }

    private fun stream(buffer: Int): Int {
        var samples: Int = 0
        while (samples < BUFFER_SIZE) {
            pcm.position(samples)
            val samplesPerChannel: Int = track.getSamples(pcm)
            if (samplesPerChannel == 0) {
                break
            }
            samples += samplesPerChannel * track.channels
        }
        if (samples != 0) {
            pcm.position(0)
            pcm.limit(samples)
            AL10.alBufferData(buffer, format, pcm, track.sampleRate)
            pcm.limit(BUFFER_SIZE)
        }
        return samples
    }

    fun play(gain: Double? = null): Boolean {
        if (gain != null) {
            this.gain(gain)
        }

        for (i in 0 until buffers.limit()) {
            if (stream(buffers.get(i)) == 0) {
                return false
            }
        }
        AL10.alSourceQueueBuffers(source, buffers)
        AL10.alSourcePlay(source)
        return true
    }

    fun pause() {
        AL10.alSourcePause(source)
    }

    fun stop() {
        AL10.alSourceStop(source)
    }

    fun gain(gain: Double) {
        AL10.alSourcef(source, AL_GAIN, gain.toFloat())
    }

    fun pitch(pitch: Double) {
        AL10.alSourcef(source, AL_PITCH, pitch.toFloat())
    }

    fun update(loop: Boolean): Boolean {
        val processed: Int = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED)
        for (i in 0 until processed) {
            bufferOffset += (BUFFER_SIZE / track.channels).toLong()
            val buffer: Int = AL10.alSourceUnqueueBuffers(source)
            if (stream(buffer) == 0) {
                var shouldExit: Boolean = true
                if (loop) {
                    track.rewind()
                    bufferOffset = 0
                    offset = bufferOffset
                    lastOffset = offset
                    shouldExit = stream(buffer) == 0
                }
                if (shouldExit) {
                    return false
                }
            }
            AL10.alSourceQueueBuffers(source, buffer)
        }
        if (processed == 2) {
            AL10.alSourcePlay(source)
        }
        return true
    }

    val progressUpdater: ProgressUpdater
        get() = object : ProgressUpdater {
            override fun makeCurrent(current: Boolean) {
                EXTThreadLocalContext.alcSetThreadContext(if (current) AudioSystem.context else MemoryUtil.NULL)
            }

            override fun updateProgress() {
                offset = bufferOffset + AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET)
                track.progressBy((offset - lastOffset).toInt())
                lastOffset = offset
            }
        }

    companion object {
        private val BUFFER_SIZE: Int = 1024 * 8
    }
}


class AudioPlayer : Extension {
    override var enabled = true
    var audioFile = "data/audio/audio.ogg"

    var duration = 0.0
    var loop = false
    var drawProgress = true
    var useAudioClock = true
    var useScrub = true
    var showProgress = true


    var vt: VorbisTrack? = null


    private var frameTime = 0.0

    override fun setup(program: Program) {
        val f = File(audioFile)
        require(f.exists()) { "file '${audioFile}' does not exist"}

        vt = VorbisTrack(audioFile)
        vt?.play(loop = loop)
        if (useScrub) {
            listOf(program.mouse.buttonUp, program.mouse.dragged).listen {
                val dx = it.position.x / program.width
                vt?.skipTo(dx.toFloat())
            }
        }
        if (useAudioClock) {
            program.clock = { vt?.position() ?: 0.0 }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {


    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        if (showProgress) {
            drawer.isolated {
                drawer.defaults()
                val dx = (vt?.relativePosition() ?: 0.0).coerceIn(0.0, 1.0)
                drawer.rectangle(10.0, drawer.height - 20.0, (drawer.width - 20.0) * dx, 10.0)
            }
        }
    }

}