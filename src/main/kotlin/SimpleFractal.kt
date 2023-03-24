import org.openrndr.Program
import org.openrndr.math.Vector2
import kotlin.math.sqrt

fun Program.simpleFractal(depth: Int, p1: Vector2, p2: Vector2) {
    if (depth == 0) return drawer.lineSegment(p1, p2)
    val p112 = Vector2((p1.x * 2 + p2.x) / 3, (p1.y * 2 + p2.y) / 3)
    val p122 = Vector2((p1.x + p2.x * 2) / 3, (p1.y + p2.y * 2) / 3)
    val middle = Vector2((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
    val cos = sqrt(3.0) / 2
    val p3 = Vector2(middle.x - (p122.y - p112.y) * cos, middle.y + (p122.x - p112.x) * cos)
    simpleFractal(depth - 1, p1, p112)
    simpleFractal(depth - 1, p112, p3)
    simpleFractal(depth - 1, p3, p122)
    simpleFractal(depth - 1, p122, p2)
}