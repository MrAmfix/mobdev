package io.github.mobdev.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val MAX_DIMENSION = 1600
    private const val MAX_BYTES = 800 * 1024
    private const val MIN_QUALITY = 50
    private const val INITIAL_QUALITY = 85
    private const val QUALITY_STEP = 10

    /**
     * Read an image from [uri], downscale long side to [MAX_DIMENSION] and
     * recompress to JPEG, lowering quality until the payload is under [MAX_BYTES]
     * (or [MIN_QUALITY] is reached). Returns null on failure.
     */
    fun compressToJpeg(resolver: ContentResolver, uri: Uri): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodeStream = resolver.openInputStream(uri) ?: return null
        val raw = decodeStream.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val scaled = scaleToMaxDimension(raw, MAX_DIMENSION).also {
            if (it !== raw) raw.recycle()
        }

        return try {
            var quality = INITIAL_QUALITY
            var bytes: ByteArray
            while (true) {
                val buffer = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
                bytes = buffer.toByteArray()
                if (bytes.size <= MAX_BYTES || quality <= MIN_QUALITY) break
                quality -= QUALITY_STEP
            }
            bytes
        } finally {
            scaled.recycle()
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim || h / 2 >= maxDim) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
