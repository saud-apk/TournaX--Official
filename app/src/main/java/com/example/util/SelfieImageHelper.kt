package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object SelfieImageHelper {

    fun createTempSelfieUri(context: Context): Uri {
        val dir = File(context.cacheDir, "selfies").apply {
            if (!exists()) mkdirs()
        }
        val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    suspend fun encodeBitmapToBase64(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        mirrorHorizontal: Boolean = true,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val matrix = Matrix().apply {
                if (rotationDegrees != 0) {
                    postRotate(rotationDegrees.toFloat())
                }
                if (mirrorHorizontal) {
                    postScale(-1f, 1f)
                }
            }
            val oriented = if (rotationDegrees != 0 || mirrorHorizontal) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            if (oriented !== bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            compressAndEncodeBitmap(oriented)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to process captured face frame.", e))
        }
    }

    suspend fun encodeSelfieUriToBase64(
        context: Context,
        uri: Uri,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                return@withContext Result.failure(IllegalStateException("Could not read captured selfie image."))
            }

            val maxSourceDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
            var sampleSize = 1
            while (maxSourceDim / sampleSize > 720) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalStateException("Captured selfie bitmap was empty."))

            compressAndEncodeBitmap(decoded)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to process selfie photo.", e))
        }
    }

    private fun compressAndEncodeBitmap(source: Bitmap): Result<String> {
        val targetMax = 380
        val maxDim = max(source.width, source.height)
        val scaled = if (maxDim > targetMax) {
            val ratio = targetMax.toFloat() / maxDim.toFloat()
            val newW = (source.width * ratio).toInt().coerceAtLeast(1)
            val newH = (source.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, newW, newH, true).also {
                if (it !== source && !source.isRecycled) source.recycle()
            }
        } else {
            source
        }

        var quality = 65
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 15
        } while (bytes.size > 160_000 && quality >= 25)

        if (!scaled.isRecycled) {
            scaled.recycle()
        }

        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return if (encoded.length < 100 || encoded.length > 340_000) {
            Result.failure(IllegalStateException("Selfie image size out of allowed bounds. Please retry."))
        } else {
            Result.success(encoded)
        }
    }

    fun decodeSelfieBase64(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val cleaned = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
