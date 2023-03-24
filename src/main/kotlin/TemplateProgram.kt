import org.openrndr.application
import org.openrndr.ffmpeg.ScreenRecorder
import java.time.Duration
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private fun Duration.render() = toString()
    .substring(2)
    .replace("(\\d[HMS])(?!$)", "$1 ")
    .toLowerCase()

@OptIn(ExperimentalTime::class)
fun main() {
    val start = Instant.now()
    var lastMeasuredTime = start
    var lastMeasuredFrames = 0
    measureTime {
        application {
            configure {
                width = 768
                height = 576
            }

            val recorder = ScreenRecorder().apply {
                frameRate = 60
                maximumFrames = frameRate * 30L
                outputFile = "output.mp4"
            }

            program {
                extend(recorder)
                extend {
                    drawLandscape()
                    if (frameCount % 10 == 0 && frameCount != 0) {
                        val now = Instant.now()
                        val fps =
                            (frameCount - lastMeasuredFrames) / ((now.toEpochMilli() - lastMeasuredTime.toEpochMilli()) / 1000.0)
                        val totalElapsedTime = Duration.ofMillis(now.toEpochMilli() - start.toEpochMilli()).render()
                        val timeLeft = when {
                            fps == 0.0 -> "Infinity"
                            frameCount.toLong() == recorder.maximumFrames -> "0"
                            else -> Duration.ofSeconds(((recorder.maximumFrames - frameCount.toLong()) / fps).toLong())
                                .render()
                        }
                        println("Frame $frameCount / ${recorder.maximumFrames} (${frameCount * 100 / recorder.maximumFrames}%). Elapsed time: ${totalElapsedTime}s. FPS: $fps. Time left: ${timeLeft}.")
                        lastMeasuredTime = now
                        lastMeasuredFrames = frameCount
                    }
                }
            }
        }
    }.let { println("Finished after: $it") }
}
