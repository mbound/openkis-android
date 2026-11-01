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
import kotlin.math.max

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

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || points.isEmpty()) return

        val projection = mapView.projection
        val boundingBox = mapView.boundingBox
        val density = mapView.resources.displayMetrics.density
        val scaledDensity = mapView.resources.displayMetrics.scaledDensity
        val zoom = mapView.zoomLevelDouble
        val showLabels = zoom >= 15.0
        val cellPx = when {
            zoom < 8.0 -> 24f * density
            zoom < 11.0 -> 16f * density
            zoom < 13.0 -> 10f * density
            else -> 0f
        }
        val occupiedCells = if (cellPx > 0f) HashSet<Long>() else null

        labelTextPaint.textSize = 11f * scaledDensity
        val labelMetrics = labelTextPaint.fontMetrics
        val labelHeight = labelMetrics.bottom - labelMetrics.top
        val labelPaddingX = 4f * density
        val labelPaddingY = 2f * density
        val labelOffset = 6f * density
        var labelsDrawn = 0

        for (point in points) {
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
            if (!selected && occupiedCells != null) {
                val cellX = (screenPoint.x / cellPx).toInt()
                val cellY = (screenPoint.y / cellPx).toInt()
                val key = (cellX.toLong() shl 32) xor (cellY.toLong() and 0xffffffffL)
                if (!occupiedCells.add(key)) continue
            }

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

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        if (points.isEmpty()) return false

        val projection = mapView.projection
        val boundingBox = mapView.boundingBox
        val density = mapView.resources.displayMetrics.density
        val radius = max(28f * density, 36f)
        val radiusSquared = radius * radius
        var bestPoint: MapRenderPoint? = null
        var bestDistance = Float.MAX_VALUE

        for (point in points) {
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
