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
    val coef = atan(seconds / 30) / PI + 0.5
    val grassHeight = 100
    drawGrass(random, grassHeight, count = 10000)

    val bottomLeft = Vector2((width - treeWidth) / 2, height.toDouble() - grassHeight / 2)
    val bottomRight = Vector2((width + treeWidth) / 2, height.toDouble() - grassHeight / 2)
    drawFractalTree(
        min(12, 1 + (seconds / 2).toInt()),
        bottomLeft,
        bottomRight,
        10 + 150.0 * coef,
        0.15 + coef * 0.55
    )
    drawGrassForeground(1000, Random(1000), IntRectangle(0, bottomLeft.y.toInt(), width, 5))
}

private fun Program.generatePolygonTopPoints(polygon: StemRectangle): List<Vector2> {
    val baseAngle = atan2(
        polygon.bottomRight.y - polygon.bottomLeft.y,
        polygon.bottomRight.x - polygon.bottomLeft.x
    ).toDegrees()
    val additionalAngle = asin(sin(seconds / 3).let { abs(it).pow(0.5).withSign(it) }).toDegrees() / 90
    val angle = baseAngle + additionalAngle
    val center = (polygon.bottomLeft + polygon.bottomRight) / 2.0
    val rotationMatrix = Matrix44.rotateZ(angle)
    val width = polygon.bottomLeft.distanceTo(polygon.bottomRight)
    val points = listOf(
        Vector2(-width / 2, -polygon.height),
        Vector2(additionalAngle, -width * 0.7 - polygon.height),
        Vector2(+width / 2, -polygon.height)
    )
    return points.map { (rotationMatrix * it.xy01).xy + center }
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
    val starPoints = Array(count) {
        Vector2(
            random.nextDouble(drawer.width.toDouble()),
            random.nextDouble(drawer.height.toDouble())
        )
    }
    drawer.strokeWeight = 0.0
    drawer.fill = ColorRGBa.LIGHT_YELLOW.opacify(max(0.0, -cos(seconds)))
    for (star in starPoints) {
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
    val grassStarts = Array(count) {
        Vector2(
            random.nextDouble(rectangle.corner.x + rectangle.width.toDouble()),
            rectangle.corner.y + random.nextDouble(rectangle.height.toDouble())
        )
    }

    drawer.strokeWeight = 2.0
    for (grassStart in grassStarts) {
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

private fun Program.drawFractalTree(
    depth: Int, bottomLeft: Vector2, bottomRight: Vector2, height: Double, heightCoefficient: Double,
) {
    val topPoints = generatePolygonTopPoints(StemRectangle(bottomLeft, bottomRight, height))
    drawer.strokeWeight = 0.0
    drawer.fill = makeDayOrNight(ColorRGBa.SADDLE_BROWN)
    val topLeft = topPoints.first()
    val topRight = topPoints.last()
    val polygonPoints = object : AbstractList<Vector2>() {
        override val size: Int = topPoints.size + 2

        override fun get(index: Int): Vector2 = when (index) {
            0 -> bottomLeft
            lastIndex -> bottomRight
            else -> topPoints[index - 1]
        }
    }
    drawer.contour(ShapeContour.fromPoints(polygonPoints, closed = true))
    drawer.strokeWeight = 2.0
    drawer.stroke = makeDayOrNight(ColorRGBa.MAROON)
    drawer.lineSegment(bottomLeft, topLeft)
    drawer.lineSegment(topRight, bottomRight)
    drawer.strokeWeight = 0.0
    for ((p1, p2) in topPoints.zipWithNext()) {
        if (depth == 0) {
            makeLeaf(p1, p2)
        } else {
            drawFractalTree(
                depth = depth - 1,
                bottomLeft = p1,
                bottomRight = p2,
                height = height * heightCoefficient,
                heightCoefficient = heightCoefficient,
            )
        }
    }
}
