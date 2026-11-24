package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.FilterSettings
import com.example.model.MediaClip
import com.example.model.MediaType
import com.example.model.MotionStudioUiState
import com.example.model.TextLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object MediaUtils {

    suspend fun resolveMediaMetadata(context: Context, uri: Uri): MediaClip = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri).orEmpty()
        var displayName = "Media_${System.currentTimeMillis() % 10000}"

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                }
            }
        }

        val isVideo = mimeType.startsWith("video") ||
            displayName.endsWith(".mp4", ignoreCase = true) ||
            displayName.endsWith(".mov", ignoreCase = true) ||
            displayName.endsWith(".webm", ignoreCase = true) ||
            displayName.endsWith(".mkv", ignoreCase = true)

        val durationMs = if (isVideo) {
            extractVideoDurationMs(context, uri)
        } else {
            0L
        }

        MediaClip(
            uri = uri,
            displayName = displayName,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
            durationMs = durationMs,
            mimeType = mimeType,
        )
    }

    private fun extractVideoDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun loadClipThumbnail(context: Context, clip: MediaClip): Bitmap? = withContext(Dispatchers.IO) {
        if (clip.mediaType == MediaType.VIDEO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, clip.uri)
                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        0L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        320,
                        240,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    retriever.getFrameAtTime(0L)
                }
                frame
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            decodeScaledBitmap(context, clip.uri, maxDim = 320)
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDim * 2 || bounds.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) {
            null
        }
    }

    fun buildAndroidColorMatrix(
        contrast: Float,
        saturation: Float,
        brightness: Float,
    ): ColorMatrix {
        val result = ColorMatrix()

        // 1. Saturation
        if (saturation != 1f) {
            val satMatrix = ColorMatrix().apply { setSaturation(saturation.coerceIn(0f, 3f)) }
            result.postConcat(satMatrix)
        }

        // 2. Contrast (centered around 128)
        if (contrast != 1f) {
            val c = contrast.coerceIn(0.2f, 2.5f)
            val translate = (-0.5f * c + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translate,
                    0f, c, 0f, 0f, translate,
                    0f, 0f, c, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
            result.postConcat(contrastMatrix)
        }

        // 3. Brightness (multiplicative scale matching CSS brightness)
        if (brightness != 1f) {
            val b = brightness.coerceIn(0.2f, 2.5f)
            val brightMatrix = ColorMatrix(
                floatArrayOf(
                    b, 0f, 0f, 0f, 0f,
                    0f, b, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
            result.postConcat(brightMatrix)
        }

        return result
    }

    suspend fun renderSnapshotBitmap(
        context: Context,
        clip: MediaClip?,
        filters: FilterSettings,
        textLayers: List<TextLayer>,
        videoPositionMs: Long = 0L,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val baseBitmap = if (clip == null) {
            Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.parseColor("#141923"))
            }
        } else if (clip.mediaType == MediaType.VIDEO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, clip.uri)
                val timeUs = (videoPositionMs.coerceAtLeast(0L)) * 1000L
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L)
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            decodeScaledBitmap(context, clip.uri, maxDim = 1440)
        } ?: return@withContext null

        val outBitmap = Bitmap.createBitmap(
            baseBitmap.width.coerceAtLeast(1),
            baseBitmap.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(outBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            val matrix = buildAndroidColorMatrix(
                contrast = filters.contrast,
                saturation = filters.saturation,
                brightness = filters.brightness,
            )
            colorFilter = ColorMatrixColorFilter(matrix)
        }

        if (filters.blurRadiusDp > 0.5f) {
            // Fast box-downscale-upscale approximation for portable offscreen snapshot export
            val factor = (1f + filters.blurRadiusDp * 0.6f).coerceIn(1.5f, 10f)
            val smallW = (baseBitmap.width / factor).roundToInt().coerceAtLeast(8)
            val smallH = (baseBitmap.height / factor).roundToInt().coerceAtLeast(8)
            val small = Bitmap.createScaledBitmap(baseBitmap, smallW, smallH, true)
            val blurred = Bitmap.createScaledBitmap(small, baseBitmap.width, baseBitmap.height, true)
            canvas.drawBitmap(blurred, 0f, 0f, paint)
            small.recycle()
            blurred.recycle()
        } else {
            canvas.drawBitmap(baseBitmap, 0f, 0f, paint)
        }

        // Scale text relative to a ~360dp stage width so exported frames match preview proportions
        val scaleFactor = outBitmap.width / 360f
        textLayers.filter { it.isVisible && it.text.isNotBlank() }.forEach { layer ->
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.color.argb.toInt()
                textSize = layer.fontSizeSp * scaleFactor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(6f * scaleFactor / 2f, 0f, 2f * scaleFactor / 2f, android.graphics.Color.argb(200, 0, 0, 0))
            }

            val maxTextWidth = (outBitmap.width * 0.86f).roundToInt().coerceAtLeast(100)
            val layout = StaticLayout.Builder
                .obtain(layer.text, 0, layer.text.length, textPaint, maxTextWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .build()

            val maxLineW = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
            val centerX = layer.normalizedX.coerceIn(0.08f, 0.92f) * outBitmap.width
            val centerY = layer.normalizedY.coerceIn(0.08f, 0.92f) * outBitmap.height
            val drawLeft = centerX - (maxTextWidth / 2f)
            val drawTop = centerY - (layout.height / 2f)

            if (layer.color.hasBackdropPill) {
                val padX = 14f * scaleFactor
                val padY = 8f * scaleFactor
                val pillRect = RectF(
                    centerX - maxLineW / 2f - padX,
                    drawTop - padY,
                    centerX + maxLineW / 2f + padX,
                    drawTop + layout.height + padY,
                )
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(210, 11, 13, 18)
                }
                canvas.drawRoundRect(pillRect, 12f * scaleFactor, 12f * scaleFactor, pillPaint)
            }

            canvas.save()
            canvas.translate(drawLeft, drawTop)
            layout.draw(canvas)
            canvas.restore()
        }

        outBitmap
    }

    suspend fun exportBitmapToCache(context: Context, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "motion_studio_$stamp.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    suspend fun exportNotesToCache(context: Context, notes: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "motion-studio-project.txt")
        file.writeText(notes)
        file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun sharePlainText(context: Context, text: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Motion Studio Project Notes")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun buildProjectNotesText(state: MotionStudioUiState): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val clipLines = if (state.clips.isEmpty()) {
            "  - None"
        } else {
            state.clips.mapIndexed { index, clip ->
                val dur = if (clip.mediaType == MediaType.VIDEO && clip.durationMs > 0L) {
                    " (${formatTimecode(clip.durationMs)})"
                } else {
                    ""
                }
                "  ${index + 1}. [${clip.mediaType.name}] ${clip.displayName}$dur"
            }.joinToString("\n")
        }

        val textLines = if (state.textLayers.isEmpty()) {
            "  - None"
        } else {
            state.textLayers.mapIndexed { index, layer ->
                val visibility = if (layer.isVisible) "visible" else "hidden"
                "  ${index + 1}. \"${layer.text}\" (${layer.fontSizeSp.roundToInt()}sp, ${layer.color.label}, $visibility)"
            }.joinToString("\n")
        }

        return buildString {
            appendLine("MOTION STUDIO — Project Notes")
            appendLine("Generated: $timestamp")
            appendLine()
            appendLine("MEDIA CLIPS (${state.clips.size}):")
            appendLine(clipLines)
            appendLine()
            appendLine("TEXT LAYERS (${state.textLayers.size}):")
            appendLine(textLines)
            appendLine()
            appendLine("ACTIVE LOOK & FILTERS:")
            appendLine("  Preset: ${state.filters.preset.label}")
            appendLine("  Contrast: ${String.format(Locale.US, "%.2f", state.filters.contrast)}x")
            appendLine("  Saturation: ${String.format(Locale.US, "%.2f", state.filters.saturation)}x")
            appendLine("  Brightness: ${String.format(Locale.US, "%.2f", state.filters.brightness)}x")
            appendLine("  Blur Radius: ${String.format(Locale.US, "%.1f", state.filters.blurRadiusDp)} dp")
        }
    }

    fun formatTimecode(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
