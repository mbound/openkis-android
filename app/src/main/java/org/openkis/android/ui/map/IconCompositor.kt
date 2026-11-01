package org.openkis.android.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconCompositor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val iconSizePx = 64
    private val cache = LruCache<String, Bitmap>(64)

    /**
     * Returns the cached composited bitmap for [category]/[layers].
     * The map canvas overlay uses this directly so thousands of marker wrapper
     * objects do not need to be allocated.
     */
    fun bitmap(category: String, layers: List<String>): Bitmap {
        val key = "$category:${layers.joinToString(",")}"
        return cache[key] ?: buildBitmap(category, layers).also { cache.put(key, it) }
    }

    /**
     * Returns a fresh BitmapDrawable wrapper for UI surfaces that need a Drawable.
     */
    fun compose(category: String, layers: List<String>): BitmapDrawable =
        BitmapDrawable(context.resources, bitmap(category, layers))

    private fun buildBitmap(category: String, layers: List<String>): Bitmap {
        val result = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        for (layer in layers) {
            val path = "icons/$category/$layer.png"
            try {
                context.assets.open(path).use { stream ->
                    val src = BitmapFactory.decodeStream(stream) ?: return@use
                    canvas.drawBitmap(src, null, Rect(0, 0, iconSizePx, iconSizePx), null)
                    src.recycle()
                }
            } catch (_: Exception) {
                // Missing asset for this layer — skip silently
            }
        }
        return result
    }
}
