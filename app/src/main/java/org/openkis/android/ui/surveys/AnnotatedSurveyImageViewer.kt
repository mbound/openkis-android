package org.openkis.android.ui.surveys

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openkis.android.R
import org.openkis.android.data.local.entity.SurveyAnnotationEntity
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.local.entity.annotationKey
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun AnnotatedSurveyImageViewer(
    survey: SurveyEntity,
    file: File,
    onDismiss: () -> Unit,
    viewModel: SurveyAnnotationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val annotationFlow = remember(
        survey.serverUrl,
        survey.entityType,
        survey.dbId,
        survey.imageUrl,
        survey.thumbnailUrl,
        survey.surveyIndex
    ) { viewModel.observe(survey) }
    val annotations by annotationFlow.collectAsState(initial = emptyList())

    val pngLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = viewModel.exportAnnotatedPng(context, uri, file, annotations)
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.annotation_export_saved else R.string.annotation_export_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = viewModel.exportAnnotatedPdf(context, uri, file, annotations)
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.annotation_export_saved else R.string.annotation_export_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewerSize by remember { mutableStateOf(IntSize.Zero) }
    var annotationMode by remember { mutableStateOf(false) }
    var editingAnnotation by remember { mutableStateOf<SurveyAnnotationEntity?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }

    val imageSize = remember(file.absolutePath) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        IntSize(
            width = options.outWidth.coerceAtLeast(1),
            height = options.outHeight.coerceAtLeast(1)
        )
    }
    val imageRect = remember(viewerSize, imageSize) {
        fittedImageRect(viewerSize, imageSize)
    }

    fun beginAnnotationAt(tap: Offset): Boolean {
        if (viewerSize == IntSize.Zero || imageRect.isEmpty) return false

        val base = screenToBase(tap, viewerSize, scale, offset)
        if (!imageRect.contains(base)) return false

        val nx = ((base.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
        val ny = ((base.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
        val now = System.currentTimeMillis()
        val uuid = UUID.randomUUID()
        editingAnnotation = SurveyAnnotationEntity(
            id = uuid.toString(),
            serverUrl = survey.serverUrl,
            entityType = survey.entityType,
            dbId = survey.dbId,
            surveyKey = survey.annotationKey(),
            surveyTitle = survey.title,
            markerId = "M-" + uuid.toString().take(6).uppercase(),
            normalizedX = nx,
            normalizedY = ny,
            createdAt = now,
            updatedAt = now
        )
        editingIsNew = true
        return true
    }

    if (editingAnnotation != null) {
        AnnotationEditorDialog(
            annotation = editingAnnotation!!,
            isNew = editingIsNew,
            onDismiss = {
                editingAnnotation = null
                editingIsNew = false
            },
            onSave = {
                viewModel.save(it)
                editingAnnotation = null
                editingIsNew = false
            },
            onDelete = {
                viewModel.delete(it)
                editingAnnotation = null
                editingIsNew = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { viewerSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 8f)
                        offset = if (newScale <= 1f) Offset.Zero else offset + pan
                        scale = newScale
                    }
                }
                .pointerInput(annotationMode, scale, offset, imageRect, viewerSize) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!annotationMode) {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        },
                        onLongPress = { tap ->
                            if (beginAnnotationAt(tap)) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onTap = { tap ->
                            if (annotationMode) {
                                beginAnnotationAt(tap)
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = file,
                contentDescription = survey.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )

            if (viewerSize != IntSize.Zero && !imageRect.isEmpty) {
                annotations.forEachIndexed { index, annotation ->
                    val base = Offset(
                        x = imageRect.left + annotation.normalizedX.coerceIn(0f, 1f) * imageRect.width,
                        y = imageRect.top + annotation.normalizedY.coerceIn(0f, 1f) * imageRect.height
                    )
                    val screen = baseToScreen(base, viewerSize, scale, offset)
                    AnnotationMarker(
                        number = index + 1,
                        annotation = annotation,
                        screenPosition = screen,
                        onClick = {
                            editingAnnotation = annotation
                            editingIsNew = false
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = survey.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.annotation_marker_count, annotations.size),
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                    }
                }
                Text(
                    text = if (annotationMode) {
                        stringResource(R.string.viewer_annotation_hint)
                    } else {
                        stringResource(R.string.viewer_long_press_annotation_hint)
                    },
                    color = Color.White.copy(alpha = if (annotationMode) 1f else 0.82f),
                    style = if (annotationMode) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.labelSmall
                    },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                )
                if (!annotationMode && scale > 1f) {
                    Text(
                        text = stringResource(R.string.viewer_double_tap_reset),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { annotationMode = !annotationMode }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.viewer_annotate),
                            tint = if (annotationMode) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    Text(
                        stringResource(R.string.viewer_annotate),
                        color = if (annotationMode) MaterialTheme.colorScheme.primary else Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = Color.White)
                    }
                    Text(stringResource(R.string.share), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { pngLauncher.launch("annotated_survey.png") }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.annotation_export_png),
                            tint = Color.White
                        )
                    }
                    Text(stringResource(R.string.annotation_export_png), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { pdfLauncher.launch("annotated_survey.pdf") }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.annotation_export_pdf),
                            tint = Color.White
                        )
                    }
                    Text(stringResource(R.string.annotation_export_pdf), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AnnotationMarker(
    number: Int,
    annotation: SurveyAnnotationEntity,
    screenPosition: Offset,
    onClick: () -> Unit
) {
    val markerSize = 34.dp
    val markerSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { markerSize.toPx() }
    val markerId = annotation.markerId.ifBlank { "M" + number }
    val label = if (annotation.title.isBlank()) markerId else markerId + " - " + annotation.title

    Row(
        modifier = Modifier
            .offset {
                IntOffset(
                    (screenPosition.x - markerSizePx / 2f).roundToInt(),
                    (screenPosition.y - markerSizePx / 2f).roundToInt()
                )
            }
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(markerSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = markerId.take(7),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AnnotationEditorDialog(
    annotation: SurveyAnnotationEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (SurveyAnnotationEntity) -> Unit,
    onDelete: (SurveyAnnotationEntity) -> Unit
) {
    var markerId by remember(annotation.id) { mutableStateOf(annotation.markerId) }
    var title by remember(annotation.id) { mutableStateOf(annotation.title) }
    var category by remember(annotation.id) { mutableStateOf(annotation.category) }
    var notes by remember(annotation.id) { mutableStateOf(annotation.notes) }
    var metadata by remember(annotation.id) { mutableStateOf(annotation.metadata) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNew) R.string.annotation_new_title else R.string.annotation_edit_title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.annotation_position,
                        (annotation.normalizedX * 100f).roundToInt(),
                        (annotation.normalizedY * 100f).roundToInt()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = markerId,
                    onValueChange = { markerId = it },
                    label = { Text(stringResource(R.string.annotation_label_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.annotation_label_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.annotation_label_category)) },
                    placeholder = { Text(stringResource(R.string.annotation_category_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.annotation_label_notes)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = metadata,
                    onValueChange = { metadata = it },
                    label = { Text(stringResource(R.string.annotation_label_metadata)) },
                    placeholder = { Text(stringResource(R.string.annotation_metadata_hint)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        annotation.copy(
                            markerId = markerId.trim(),
                            title = title.trim(),
                            category = category.trim(),
                            notes = notes.trim(),
                            metadata = metadata.trim()
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = { onDelete(annotation) }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text(stringResource(R.string.remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

private fun fittedImageRect(container: IntSize, image: IntSize): Rect {
    if (container.width <= 0 || container.height <= 0 || image.width <= 0 || image.height <= 0) {
        return Rect.Zero
    }

    val containerAspect = container.width.toFloat() / container.height.toFloat()
    val imageAspect = image.width.toFloat() / image.height.toFloat()

    return if (imageAspect >= containerAspect) {
        val width = container.width.toFloat()
        val height = width / imageAspect
        val top = (container.height - height) / 2f
        Rect(0f, top, width, top + height)
    } else {
        val height = container.height.toFloat()
        val width = height * imageAspect
        val left = (container.width - width) / 2f
        Rect(left, 0f, left + width, height)
    }
}

private fun screenToBase(
    screen: Offset,
    container: IntSize,
    scale: Float,
    translation: Offset
): Offset {
    val center = Offset(container.width / 2f, container.height / 2f)
    return Offset(
        x = (screen.x - center.x - translation.x) / scale + center.x,
        y = (screen.y - center.y - translation.y) / scale + center.y
    )
}

private fun baseToScreen(
    base: Offset,
    container: IntSize,
    scale: Float,
    translation: Offset
): Offset {
    val center = Offset(container.width / 2f, container.height / 2f)
    return Offset(
        x = center.x + (base.x - center.x) * scale + translation.x,
        y = center.y + (base.y - center.y) * scale + translation.y
    )
}

private fun saveSurveyImageToGallery(context: android.content.Context, file: File): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "openkis_survey_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OpenKIS")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            uri?.let { dest ->
                context.contentResolver.openOutputStream(dest)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                true
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "OpenKIS"
            )
            dir.mkdirs()
            val dest = File(dir, "openkis_survey_${System.currentTimeMillis()}.jpg")
            file.copyTo(dest, overwrite = true)
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(dest.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            true
        }
    } catch (_: Exception) {
        false
    }
}
