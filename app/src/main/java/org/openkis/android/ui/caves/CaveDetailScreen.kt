package org.openkis.android.ui.caves

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openkis.android.R
import org.openkis.android.data.local.entity.SurveyEntity
import java.io.File
import org.openkis.android.ui.util.resolveFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaveDetailScreen(
    type: String,
    code: String,
    onBack: () -> Unit,
    viewModel: CaveDetailViewModel = hiltViewModel()
) {
    val detail by viewModel.detail.collectAsState()
    val surveysState by viewModel.surveysState.collectAsState()
    val context = LocalContext.current

    viewModel.load(type, code)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: code) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (detail != null && detail!!.latitude != 0.0) {
                        IconButton(onClick = {
                            val uri = Uri.parse("google.navigation:q=${detail!!.latitude},${detail!!.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            })
                        }) {
                            Icon(Icons.Default.Navigation, stringResource(R.string.navigate_to))
                        }
                    }
                    IconButton(onClick = {
                        detail?.let { d ->
                            val text = buildString {
                                append("${d.title}\n")
                                if (d.latitude != 0.0) append("${context.getString(R.string.label_coordinates)}: ${d.latitude}, ${d.longitude}\n")
                                d.fields.forEach { (labelRes, value) ->
                                    if (value.isNotBlank()) append("${context.getString(labelRes)}: $value\n")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, text)
                                this.type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                        }
                    }) {
                        Icon(Icons.Default.Share, stringResource(R.string.share))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = detail!!.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (detail!!.subtitle.isNotBlank()) {
                            Text(
                                text = detail!!.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (detail!!.latitude != 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "%.6f, %.6f".format(detail!!.latitude, detail!!.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Measurement highlights
                val highlights = detail!!.highlights
                if (highlights.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        highlights.forEach { (labelRes, value) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Detail fields
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var firstVisible = true
                        detail!!.fields.forEach { (labelRes, rawValue) ->
                            val displayValue = resolveFieldValue(labelRes, rawValue)
                            if (displayValue.isNotBlank()) {
                                if (!firstVisible) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                firstVisible = false
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = displayValue,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                // Surveys section (only for caves and artificials)
                if (detail!!.entityType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SurveysSection(
                        state = surveysState,
                        onDownload = { viewModel.downloadSurveys() },
                        onClear = { viewModel.clearSurveys() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SurveysSection(
    state: SurveysState,
    onDownload: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.section_surveys),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        when (state) {
            is SurveysState.Idle -> {
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.btn_download_surveys))
                }
            }

            is SurveysState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            stringResource(R.string.surveys_fetching),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is SurveysState.Loaded -> {
                if (state.surveys.isEmpty()) {
                    Text(
                        stringResource(R.string.surveys_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.surveys.forEach { survey ->
                        SurveyCard(survey = survey)
                    }
                    OutlinedButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.btn_refresh_surveys))
                    }
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.btn_clear_surveys))
                    }
                }
            }

            is SurveysState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (!state.message.contains("no server ID")) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyCard(survey: SurveyEntity) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }

    if (showFullscreen && survey.localImagePath.isNotBlank()) {
        SurveyImageViewer(
            file = File(survey.localImagePath),
            title = survey.title,
            onDismiss = { showFullscreen = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (survey.title.isNotBlank()) {
                Text(
                    text = survey.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val imageModel: Any? = when {
                survey.localImagePath.isNotBlank() -> File(survey.localImagePath)
                survey.thumbnailUrl.isNotBlank() -> survey.thumbnailUrl
                else -> null
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = survey.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (survey.localImagePath.isNotBlank()) {
                                showFullscreen = true
                            } else if (survey.imageUrl.isNotBlank()) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(survey.imageUrl))
                                )
                            }
                        },
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val surveyFields = listOfNotNull(
                survey.date.takeIf { it.isNotBlank() }?.let { R.string.survey_label_date to it },
                survey.author.takeIf { it.isNotBlank() }?.let { R.string.survey_label_author to it },
                survey.surveyors.takeIf { it.isNotBlank() }?.let { R.string.survey_label_surveyors to it },
                survey.speleoGroups.takeIf { it.isNotBlank() }?.let { R.string.survey_label_groups to it },
                survey.license.takeIf { it.isNotBlank() }?.let { R.string.survey_label_license to it },
                survey.bibliography.takeIf { it.isNotBlank() }?.let { R.string.survey_label_bibliography to it }
            )

            surveyFields.forEachIndexed { i, (labelRes, value) ->
                if (i > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SurveyImageViewer(file: File, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = file,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 8f)
                            offset = if (newScale <= 1f) Offset.Zero else offset + pan
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        })
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                if (scale > 1f) {
                    Text(
                        text = stringResource(R.string.viewer_double_tap_reset),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                }

                IconButton(onClick = {
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) { saveImageToGallery(context, file) }
                        val msg = if (saved) context.getString(R.string.viewer_saved)
                                  else context.getString(R.string.viewer_save_failed)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.viewer_save), tint = Color.White)
                }
            }
        }
    }
}

private fun saveImageToGallery(context: android.content.Context, file: File): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "openkis_survey_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OpenKIS")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { dest ->
                context.contentResolver.openOutputStream(dest)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                true
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenKIS")
            dir.mkdirs()
            val dest = File(dir, "openkis_survey_${System.currentTimeMillis()}.jpg")
            file.copyTo(dest, overwrite = true)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("image/jpeg"), null)
            true
        }
    } catch (e: Exception) {
        false
    }
}
