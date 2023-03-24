import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    oliveProgram {
        extend {
            drawer.clear(ColorRGBa.PINK)
            simpleFractal(6, Vector2(0.0, height * 1.0 / 3), Vector2(width.toDouble(), height * 1.0 / 3))
//            drawLandscape()
        }
    }
}

