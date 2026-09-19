package org.openkis.android.data.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import org.openkis.android.data.local.entity.SurveyAnnotationEntity
import java.io.File
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import javax.inject.Inject

class SurveyAnnotationExporter @Inject constructor() {

    fun exportCsv(output: OutputStream, annotations: List<SurveyAnnotationEntity>) {
        output.bufferedWriter().use { writer ->
            writer.appendLine(
                "server_url,entity_type,entity_db_id,survey_key,survey_title,marker_id,name,category," +
                    "normalized_x,normalized_y,notes,metadata,created_at,updated_at"
            )
            annotations.forEach { a ->
                writer.appendLine(
                    listOf(
                        a.serverUrl,
                        a.entityType,
                        a.dbId,
                        a.surveyKey,
                        a.surveyTitle,
                        a.markerId,
                        a.title,
                        a.category,
                        a.normalizedX.toString(),
                        a.normalizedY.toString(),
                        a.notes,
                        a.metadata,
                        a.createdAt.toString(),
                        a.updatedAt.toString()
                    ).joinToString(",") { csv(it) }
                )
            }
        }
    }

    fun exportAnnotatedPng(
        output: OutputStream,
        imageFile: File,
        annotations: List<SurveyAnnotationEntity>
    ) {
        val bitmap = renderAnnotatedBitmap(imageFile, annotations)
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not encode annotated PNG"
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun exportAnnotatedPdf(
        output: OutputStream,
        imageFile: File,
        annotations: List<SurveyAnnotationEntity>
    ) {
        val bitmap = renderAnnotatedBitmap(imageFile, annotations)
        val document = PdfDocument()
        try {
            val landscape = bitmap.width >= bitmap.height
            val pageWidth = if (landscape) 842 else 595
            val pageHeight = if (landscape) 595 else 842
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            )

            page.canvas.drawColor(Color.WHITE)
            val scale = min(
                pageWidth.toFloat() / bitmap.width.toFloat(),
                pageHeight.toFloat() / bitmap.height.toFloat()
            )
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val left = (pageWidth - drawWidth) / 2f
            val top = (pageHeight - drawHeight) / 2f
            page.canvas.drawBitmap(
                bitmap,
                null,
                RectF(left, top, left + drawWidth, top + drawHeight),
                Paint(Paint.ANTI_ALIAS_FLAG)
            )
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
            bitmap.recycle()
        }
    }

    private fun renderAnnotatedBitmap(
        imageFile: File,
        annotations: List<SurveyAnnotationEntity>
    ): Bitmap {
        val bitmap = decodeForExport(imageFile)
        val canvas = Canvas(bitmap)

        val radius = max(16f, min(bitmap.width, bitmap.height) * 0.012f)
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 35, 35)
            style = Paint.Style.FILL
        }
        val markerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = max(3f, radius * 0.15f)
        }
        val markerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.85f
            isFakeBoldText = true
        }
        val labelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = radius * 0.9f
            isFakeBoldText = true
        }
        val labelBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 0, 0, 0)
            style = Paint.Style.FILL
        }

        annotations.forEachIndexed { index, annotation ->
            val x = annotation.normalizedX.coerceIn(0f, 1f) * bitmap.width
            val y = annotation.normalizedY.coerceIn(0f, 1f) * bitmap.height
            val markerId = annotation.markerId.ifBlank { "M" + (index + 1) }
            val label = if (annotation.title.isBlank()) {
                markerId
            } else {
                markerId + " - " + annotation.title
            }

            canvas.drawCircle(x, y, radius, markerPaint)
            canvas.drawCircle(x, y, radius, markerBorder)

            val markerBaseline = y - (markerText.ascent() + markerText.descent()) / 2f
            canvas.drawText(markerId.take(7), x, markerBaseline, markerText)

            val padding = radius * 0.35f
            val textWidth = labelText.measureText(label)
            val labelHeight = labelText.fontMetrics.run { bottom - top } + padding * 2f
            var labelLeft = x + radius + padding
            var labelTop = y - labelHeight / 2f
            if (labelLeft + textWidth + padding * 2f > bitmap.width) {
                labelLeft = x - radius - padding - textWidth - padding * 2f
            }
            labelLeft = labelLeft.coerceAtLeast(0f)
            labelTop = labelTop.coerceIn(0f, max(0f, bitmap.height - labelHeight))
            val rect = RectF(
                labelLeft,
                labelTop,
                min(bitmap.width.toFloat(), labelLeft + textWidth + padding * 2f),
                labelTop + labelHeight
            )
            canvas.drawRoundRect(rect, padding, padding, labelBackground)
            val labelBaseline = labelTop + padding - labelText.fontMetrics.top
            canvas.drawText(label, labelLeft + padding, labelBaseline, labelText)
        }

        return bitmap
    }

    private fun decodeForExport(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not read survey image" }

        var sample = 1
        val maxDimension = 4096
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: error("Could not decode survey image")
        if (decoded.isMutable) return decoded

        val mutable = decoded.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("Could not prepare survey image for annotation export")
        decoded.recycle()
        return mutable
    }

    private fun csv(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""
}
