package lib

import org.openrndr.Extension
import org.openrndr.KEY_TAB
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.listen
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.math.max

//
class BrushMapper(val multisample: BufferMultisample = BufferMultisample.Disabled) : Extension {
    override var enabled: Boolean = true

    var paintMode = false

    var uvmapFile = "data/brushmap.exr"

    lateinit var uvmap: ColorBuffer

    lateinit var uvtarget: RenderTarget
    private var renderTarget: RenderTarget? = null
    private var resolvedColorBuffer: ColorBuffer? = null

    var sourceAnchor = Vector2.ZERO
    var targetAnchor: Vector2? = null
    var mousePosition = Vector2.ZERO
    var painting = false
    val uvmapper = UVMap()
    lateinit var uvmapped: ColorBuffer

    var sourceRadius = 20.0
    var targetRadius = 20.0
    override fun setup(program: Program) {
        uvmap = colorBuffer(program.width, program.height, type = ColorType.FLOAT32)

        uvmapped = colorBuffer(program.width, program.height)
        val s = uvmap.shadow
        val w = program.width
        val h = program.height
        for (y in 0 until program.height) {
            for (x in 0 until program.width) {
                s[x, y] = ColorRGBa(x.toDouble() / w, y.toDouble() / h, 0.0, 1.0)
            }
        }
        s.upload()

        uvtarget = renderTarget(program.width, program.height, program.window.contentScale) {
            colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
        }
        program.drawer.isolatedWithTarget(uvtarget) {
            clear(ColorRGBa.TRANSPARENT)
        }
        if (File(uvmapFile).exists()) {
            val l = loadImage(File(uvmapFile))
            l.copyTo(uvtarget.colorBuffer(0))
            l.destroy()
        }


        program.keyboard.keyDown.listen {
            if (it.key == KEY_TAB) {
                paintMode = !paintMode
            }

        }
        program.keyboard.character.listen {
            if (it.character == 's') {
                println("yo saving file?")
                uvtarget.colorBuffer(0).saveToFile(File(uvmapFile), async = false)

            }
            if (it.character == 'q') {
                program.drawer.isolatedWithTarget(uvtarget) {
                    clear(ColorRGBa.TRANSPARENT)
                }
            }
            if (it.character == 'l') {
                val l = loadImage(File(uvmapFile))
                l.copyTo(uvtarget.colorBuffer(0))
                l.destroy()
                //target.colorBuffer(0).saveToFile(File("data/brushmap.png"))
            }
            if (it.character == 'z') {
                sourceRadius = max(1.0, sourceRadius - 1.0)
            }
            if (it.character == 'x') {
                sourceRadius = sourceRadius + 1.0
            }

            if (it.character == 'c') {
                targetRadius = max(1.0, targetRadius - 1.0)
            }
            if (it.character == 'v') {
                targetRadius = targetRadius + 1.0
            }
        }

        program.mouse.moved.listen {
            mousePosition = it.position

        }

        program.mouse.buttonUp.listen {
            painting = false
            it.cancelPropagation()
        }

        listOf(program.mouse.buttonDown, program.mouse.dragged).listen {
            it.cancelPropagation()

            mousePosition = it.position
            if (!paintMode) {
                sourceAnchor = it.position
                targetAnchor = null
            } else {
                painting = true
                if (targetAnchor == null) {
                    targetAnchor = it.position
                }
                program.drawer.isolatedWithTarget(uvtarget) {
                    ortho(uvtarget)

                    val sourcePosition = sourceAnchor + (it.position - targetAnchor!!)
                    val targetPosition = it.position
                    val sourceRect = Rectangle.fromCenter(sourcePosition, sourceRadius*2, sourceRadius*2)
                    val targetRect = Rectangle.fromCenter(targetPosition, targetRadius*2, targetRadius*2)
                    shadeStyle = shadeStyle {
                        fragmentTransform = """
                            float d = length(c_boundsPosition.xy - vec2(0.5))*2.0;
                            x_fill *= smoothstep(0.99, 0.98, d);
                        """.trimIndent()
                    }
                    image(uvmap, sourceRect, targetRect)
                }
            }

        }



    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (program.width > 0 && program.height > 0) {    // only if the window is not minimised
            if (renderTarget == null || renderTarget?.width != program.width || renderTarget?.height != program.height) {
                renderTarget?.let {
                    it.colorBuffer(0).destroy()
                    it.detachColorAttachments()
                    it.destroy()
                }

                renderTarget = renderTarget(program.width, program.height, program.window.contentScale, multisample) {
                    colorBuffer()
                    depthBuffer()
                }

                if (multisample != BufferMultisample.Disabled) {
                    resolvedColorBuffer?.destroy()
                    resolvedColorBuffer = colorBuffer(program.width, program.height, program.window.contentScale)
                }

                renderTarget?.let {
                    drawer.withTarget(it) {
                        clear(program.backgroundColor ?: ColorRGBa.TRANSPARENT)

                    }
                }
            }
        }
        renderTarget?.bind()
    }
    override fun afterDraw(drawer: Drawer, program: Program) {
        renderTarget?.unbind()


        drawer.isolated {
            drawer.defaults()
            if (paintMode) {
                drawer.image(uvtarget.colorBuffer(0))

                uvmapper.apply(renderTarget!!.colorBuffer(0), uvtarget.colorBuffer(0), uvmapped)
                drawer.image(uvmapped)
                if (painting) {
                    val delta = mousePosition - targetAnchor!!
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.MAGENTA
                    drawer.circle(sourceAnchor + delta, targetRadius)
                }
            } else {
                drawer.image(renderTarget!!.colorBuffer(0))
                drawer.fill = null
                drawer.stroke = ColorRGBa.MAGENTA

                drawer.circle(sourceAnchor, sourceRadius)
            }
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.circle(mousePosition, sourceRadius)
            drawer.circle(mousePosition, targetRadius)
        }

    }

}