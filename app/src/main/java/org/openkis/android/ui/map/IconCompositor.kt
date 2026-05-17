package org.openkis.android.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconCompositor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val iconSizePx = 128
    private val cache = LruCache<String, Bitmap>(64)

    /**
     * Returns a fresh BitmapDrawable wrapping the composited icon for [category]/[layers].
     * A new wrapper is returned each call so callers can safely apply per-marker ColorFilters
     * (e.g. amber selection tint) without affecting other markers that share the same bitmap.
     */
    fun compose(category: String, layers: List<String>): BitmapDrawable {
        val key = "$category:${layers.joinToString(",")}"
        val bitmap = cache[key] ?: buildBitmap(category, layers).also { cache.put(key, it) }
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun buildBitmap(category: String, layers: List<String>): Bitmap {
        val result = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        for (layer in layers) {
            val path = "icons/$category/$layer.png"
            try {
                context.assets.open(path).use { stream ->
                    val src = BitmapFactory.decodeStream(stream) ?: return@use
                    canvas.drawBitmap(src, 0f, 0f, null)
                    src.recycle()
                }
            } catch (_: Exception) {
                // Missing asset for this layer — skip silently
            }
        }
        return result
    }
}
