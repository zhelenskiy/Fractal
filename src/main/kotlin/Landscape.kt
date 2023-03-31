import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.times
import org.openrndr.math.transforms.rotateZ
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.*
import kotlin.random.Random

fun Program.drawLandscape() {
    drawer.clear(ColorRGBa.BLACK)
    val random = Random(100)
    drawSky(random, 1000)
    val treeWidth = min(log2(seconds + 1).pow(2), 20.0)
    val offset = 2.0
    val coef = atan((seconds + offset) / 30) / PI + 0.5
    val grassHeight = 100
    drawGrass(random, grassHeight, count = 10000)

    val bottomLeft = Vector2((width - treeWidth) / 2, height.toDouble() - grassHeight / 2)
    val bottomRight = Vector2((width + treeWidth) / 2, height.toDouble() - grassHeight / 2)
    drawFractalTree(
        min(10, ((seconds + offset).pow(0.8)).toInt()),
        bottomLeft,
        bottomRight,
        10 + 200.0 * coef,
        0.15 + coef * 0.65
    )
    drawGrassForeground(1000, Random(1000), IntRectangle(0, bottomLeft.y.toInt(), width, 5))
}

private fun Program.generatePolygon(polygon: StemRectangle): ShapeContour {
    val baseAngle = atan2(
        polygon.bottomRight.y - polygon.bottomLeft.y,
        polygon.bottomRight.x - polygon.bottomLeft.x
    ).toDegrees()
    val additionalAngle = asin(sin(seconds / 3).let { abs(it).pow(0.5).withSign(it) }).toDegrees() / 90
    val angle = baseAngle + additionalAngle
    val center = (polygon.bottomLeft + polygon.bottomRight) / 2.0
    val rotationMatrix = Matrix44.rotateZ(angle)
    val width = polygon.bottomLeft.distanceTo(polygon.bottomRight)
    fun Vector2.rotated() = (rotationMatrix * xy01).xy + center
    val points = listOf(
        polygon.bottomLeft,
        Vector2(-width / 2, -polygon.height).rotated(),
        Vector2(additionalAngle, -width * 0.7 - polygon.height).rotated(),
        Vector2(+width / 2, -polygon.height).rotated(),
        polygon.bottomRight,
    )
    return ShapeContour.fromPoints(points, closed = true)
}

private fun Program.makeLeaf(p1: Vector2, p2: Vector2) {
    drawer.strokeWeight = 1.0
    drawer.stroke = makeDayOrNight(ColorRGBa.BLACK)
    drawer.fill = makeDayOrNight(ColorRGBa.DARK_GREEN)
    drawer.circle((p1 + p2) / 2.0, 3.0)
}

fun Program.makeDayOrNight(colorRGBa: ColorRGBa) = colorRGBa.shade(cos(seconds) * 0.25 + 0.75)

private fun Program.drawSky(random: Random, starsCount: Int) {
    drawStars(random, starsCount)
    drawBlueSky()
    drawSun()
}

fun Program.drawStars(random: Random, count: Int) {
    drawer.strokeWeight = 0.0
    drawer.fill = ColorRGBa.LIGHT_YELLOW.opacify(max(0.0, -cos(seconds)))
    repeat(count) {
        val star = Vector2(
            random.nextDouble(drawer.width.toDouble()),
            random.nextDouble(drawer.height.toDouble())
        )
        drawer.circle(star, 1.0)
    }
}

private fun Program.drawBlueSky() {
    val blueComponent = ColorRGBa.SKY_BLUE.opacify(cos(seconds) * 0.5 + 0.5)
    val redComponent = ColorRGBa.ROSY_BROWN.opacify(cos(seconds) * 0.5 + 0.5)
    drawer.fill = abs(sin(seconds)).pow(3).let { redComponent * it + blueComponent * (1 - it) }
    drawer.strokeWeight = 0.0
    drawer.rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
}

private fun Program.drawGrass(random: Random, grassHeight: Int, count: Int) {
    drawGrassBackground(grassHeight)
    drawGrassForeground(count, random, IntRectangle(0, height - grassHeight, width, grassHeight))
}

private fun Program.drawGrassForeground(count: Int, random: Random, rectangle: IntRectangle) {
    drawer.strokeWeight = 2.0
    repeat(count) {
        val grassStart = Vector2(
            random.nextDouble(rectangle.corner.x + rectangle.width.toDouble()),
            rectangle.corner.y + random.nextDouble(rectangle.height.toDouble())
        )
        drawer.stroke = makeDayOrNight(ColorRGBa(0.0, random.nextDouble(100.0, 200.0) / 256, 0.0))
        val curHeight = random.nextDouble(3.0, 7.0)
        val additionalAngle = asin(sin(seconds / 3).let { abs(it).pow(0.5).withSign(it) }) / 3
        val cos = cos(additionalAngle)
        val sin = sin(additionalAngle)
        drawer.lineSegment(grassStart, grassStart + curHeight * Vector2(sin, -abs(cos)))
    }
}

private fun Program.drawGrassBackground(grassHeight: Int) {
    drawer.fill = makeDayOrNight(ColorRGBa.FOREST_GREEN)
    drawer.strokeWeight = 0.0
    drawer.rectangle(-1.0, height.toDouble() - grassHeight, width.toDouble() + 2.0, height.toDouble() + 1)
}

private fun Program.drawSun() {
    drawer.fill = ColorRGBa.YELLOW
    drawer.strokeWeight = 0.0
    val sunRadius = 30.0
    if (acos(cos(seconds)) <= PI / 2) {
        drawer.circle(
            Vector2(
                sin(seconds) * (width / 2 + 2 * sunRadius) + width / 2 - sunRadius,
                100 - 40 * abs(cos(seconds))
            ), sunRadius
        )
    }
}

private fun Double.toDegrees() = this * 180 / PI
private data class StemRectangle(val bottomLeft: Vector2, val bottomRight: Vector2, val height: Double)

@OptIn(ExperimentalStdlibApi::class)
private fun Program.drawFractalTree(
    depth: Int, bottomLeft: Vector2, bottomRight: Vector2, height: Double, heightCoefficient: Double,
) {
    drawer.strokeWeight = 0.0
    drawer.fill = makeDayOrNight(ColorRGBa.SADDLE_BROWN)
    val contour = generatePolygon(StemRectangle(bottomLeft, bottomRight, height))
    val topLeft = contour.segments[0].end
    val topRight = contour.segments[contour.segments.size - 2].start
    drawer.contour(contour)
    drawer.strokeWeight = 2.0
    drawer.stroke = makeDayOrNight(ColorRGBa.MAROON)
    drawer.lineSegment(bottomLeft, topLeft)
    drawer.lineSegment(topRight, bottomRight)
    drawer.strokeWeight = 0.0
    if (depth == 0) {
        makeLeaf(topLeft, topRight)
    } else {
        for (index in 1..<(contour.segments.size - 2)) {
            val segment = contour.segments[index]
            drawFractalTree(
                depth = depth - 1,
                bottomLeft = segment.start,
                bottomRight = segment.end,
                height = height * heightCoefficient,
                heightCoefficient = heightCoefficient,
            )
        }
    }
}
