package org.openkis.android.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

data class MapLabelPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String
)

class MapObjectLabelOverlay(
    private val minZoom: Double = 15.0
) : Overlay() {

    var points: List<MapLabelPoint> = emptyList()

    private val screenPoint = Point()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        isFakeBoldText = true
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || mapView.zoomLevelDouble < minZoom) return

        val density = mapView.resources.displayMetrics.density
        textPaint.textSize = 11f * mapView.resources.displayMetrics.scaledDensity
        val offsetX = 12f * density
        val paddingX = 4f * density
        val paddingY = 2f * density
        val metrics = textPaint.fontMetrics
        val textHeight = metrics.bottom - metrics.top

        for (point in points) {
            mapView.projection.toPixels(
                GeoPoint(point.latitude, point.longitude),
                screenPoint
            )

            if (
                screenPoint.x < -160 ||
                screenPoint.y < -80 ||
                screenPoint.x > canvas.width + 160 ||
                screenPoint.y > canvas.height + 80
            ) {
                continue
            }

            val label = point.label.take(42)
            val textWidth = textPaint.measureText(label)
            var left = screenPoint.x + offsetX
            if (left + textWidth + paddingX * 2 > canvas.width) {
                left = screenPoint.x - offsetX - textWidth - paddingX * 2
            }
            val top = screenPoint.y - textHeight / 2f - paddingY
            val rect = RectF(
                left,
                top,
                left + textWidth + paddingX * 2,
                top + textHeight + paddingY * 2
            )

            canvas.drawRoundRect(
                rect,
                4f * density,
                4f * density,
                backgroundPaint
            )
            canvas.drawText(
                label,
                left + paddingX,
                top + paddingY - metrics.top,
                textPaint
            )
        }
    }
}
