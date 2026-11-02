package org.openkis.android.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

data class MapRenderPoint(
    val type: String,
    val code: String,
    val name: String,
    val position: GeoPoint,
    val iconCategory: String,
    val iconLayers: List<String>
)

class MapObjectOverlay(
    private val compositor: IconCompositor,
    private val onObjectSelected: (type: String, code: String) -> Unit
) : Overlay() {

    var points: List<MapRenderPoint> = emptyList()
    var selectedType: String? = null
    var selectedCode: String? = null

    private val screenPoint = Point()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 213, 79)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        isFakeBoldText = true
    }
    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(185, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private var sampledSource: List<MapRenderPoint>? = null
    private var sampledZoomFloor: Int = Int.MIN_VALUE
    private var sampledCellDp: Int = Int.MIN_VALUE
    private var sampledPoints: List<MapRenderPoint> = emptyList()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || points.isEmpty()) return

        val projection = mapView.projection
        val boundingBox = mapView.boundingBox
        val density = mapView.resources.displayMetrics.density
        val scaledDensity = mapView.resources.displayMetrics.scaledDensity
        val zoom = mapView.zoomLevelDouble
        val showLabels = zoom >= 15.0
        val cellDp = when {
            zoom < 8.0 -> 24
            zoom < 11.0 -> 16
            zoom < 13.0 -> 10
            else -> 0
        }
        val pointsToDraw = stableSampledPoints(
            source = points,
            zoom = zoom,
            density = density,
            cellDp = cellDp
        )

        labelTextPaint.textSize = 11f * scaledDensity
        val labelMetrics = labelTextPaint.fontMetrics
        val labelHeight = labelMetrics.bottom - labelMetrics.top
        val labelPaddingX = 4f * density
        val labelPaddingY = 2f * density
        val labelOffset = 6f * density
        var labelsDrawn = 0

        for (point in pointsToDraw) {
            if (!boundingBox.contains(point.position)) continue

            projection.toPixels(point.position, screenPoint)
            if (
                screenPoint.x < -80 ||
                screenPoint.y < -80 ||
                screenPoint.x > canvas.width + 80 ||
                screenPoint.y > canvas.height + 80
            ) {
                continue
            }

            val selected = selectedType == point.type && selectedCode == point.code

            val bitmap = compositor.bitmap(point.iconCategory, point.iconLayers)
            val left = screenPoint.x - bitmap.width / 2f
            val top = screenPoint.y - bitmap.height / 2f

            if (selected) {
                val radius = max(bitmap.width, bitmap.height) * 0.58f
                selectionPaint.strokeWidth = max(2f, 2f * density)
                canvas.drawCircle(
                    screenPoint.x.toFloat(),
                    screenPoint.y.toFloat(),
                    radius,
                    selectionPaint
                )
            }

            canvas.drawBitmap(bitmap, left, top, bitmapPaint)

            if (showLabels && labelsDrawn < 240) {
                val label = point.name.ifBlank { point.code }.take(42)
                val textWidth = labelTextPaint.measureText(label)
                var labelLeft = screenPoint.x + bitmap.width / 2f + labelOffset
                if (labelLeft + textWidth + labelPaddingX * 2 > canvas.width) {
                    labelLeft =
                        screenPoint.x - bitmap.width / 2f - labelOffset - textWidth - labelPaddingX * 2
                }
                val labelTop = screenPoint.y - labelHeight / 2f - labelPaddingY
                val rect = RectF(
                    labelLeft,
                    labelTop,
                    labelLeft + textWidth + labelPaddingX * 2,
                    labelTop + labelHeight + labelPaddingY * 2
                )
                canvas.drawRoundRect(
                    rect,
                    4f * density,
                    4f * density,
                    labelBackgroundPaint
                )
                canvas.drawText(
                    label,
                    labelLeft + labelPaddingX,
                    labelTop + labelPaddingY - labelMetrics.top,
                    labelTextPaint
                )
                labelsDrawn++
            }
        }
    }

    private fun stableSampledPoints(
        source: List<MapRenderPoint>,
        zoom: Double,
        density: Float,
        cellDp: Int
    ): List<MapRenderPoint> {
        if (cellDp <= 0) return source

        val zoomFloor = floor(zoom).toInt().coerceIn(0, 22)
        if (
            sampledSource === source &&
            sampledZoomFloor == zoomFloor &&
            sampledCellDp == cellDp
        ) {
            return withSelectedPoint(sampledPoints, source)
        }

        val worldPixels = 256.0 * 2.0.pow(zoomFloor.toDouble())
        val cellPixels = (cellDp * density).coerceAtLeast(1f).toDouble()
        val cellsPerWorld = (worldPixels / cellPixels).coerceAtLeast(1.0)
        val occupied = HashSet<Long>()
        val sampled = ArrayList<MapRenderPoint>()

        for (point in source) {
            val lonNorm = ((point.position.longitude + 180.0) / 360.0)
                .coerceIn(0.0, 0.999999999)
            val lat = point.position.latitude.coerceIn(-85.05112878, 85.05112878)
            val latRad = Math.toRadians(lat)
            val mercatorY =
                (1.0 - ln((1.0 + sin(latRad)) / (1.0 - sin(latRad))) / (2.0 * PI)) / 2.0
            val x = floor(lonNorm * cellsPerWorld).toLong()
            val y = floor(mercatorY.coerceIn(0.0, 0.999999999) * cellsPerWorld).toLong()
            val key = (x shl 32) xor (y and 0xffffffffL)

            if (occupied.add(key)) {
                sampled += point
            }
        }

        sampledSource = source
        sampledZoomFloor = zoomFloor
        sampledCellDp = cellDp
        sampledPoints = sampled
        return withSelectedPoint(sampled, source)
    }

    private fun withSelectedPoint(
        sampled: List<MapRenderPoint>,
        source: List<MapRenderPoint>
    ): List<MapRenderPoint> {
        val type = selectedType ?: return sampled
        val code = selectedCode ?: return sampled
        if (sampled.any { it.type == type && it.code == code }) return sampled

        val selected = source.firstOrNull { it.type == type && it.code == code }
            ?: return sampled

        return sampled + selected
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        if (points.isEmpty()) return false

        val projection = mapView.projection
        val boundingBox = mapView.boundingBox
        val density = mapView.resources.displayMetrics.density
        val radius = max(28f * density, 36f)
        val radiusSquared = radius * radius
        var bestPoint: MapRenderPoint? = null
        var bestDistance = Float.MAX_VALUE

        val zoom = mapView.zoomLevelDouble
        val cellDp = when {
            zoom < 8.0 -> 24
            zoom < 11.0 -> 16
            zoom < 13.0 -> 10
            else -> 0
        }
        val pointsToHitTest = stableSampledPoints(
            source = points,
            zoom = zoom,
            density = density,
            cellDp = cellDp
        )

        for (point in pointsToHitTest) {
            if (!boundingBox.contains(point.position)) continue
            projection.toPixels(point.position, screenPoint)

            val dx = screenPoint.x - e.x
            val dy = screenPoint.y - e.y
            val distance = dx * dx + dy * dy
            if (distance <= radiusSquared && distance < bestDistance) {
                bestDistance = distance
                bestPoint = point
            }
        }

        val selected = bestPoint ?: return false
        onObjectSelected(selected.type, selected.code)
        mapView.invalidate()
        return true
    }
}
