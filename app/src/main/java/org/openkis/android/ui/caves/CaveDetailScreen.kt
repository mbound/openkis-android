package org.openkis.android.ui.caves

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import org.openkis.android.R
import org.openkis.android.data.local.entity.SurveyEntity
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
                        onDownload = { viewModel.downloadSurveys() }
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
    onDownload: () -> Unit
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
                        SurveyCard(survey = survey, onImageClick = { url ->
                            if (url.isNotBlank()) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            }
                        })
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
private fun SurveyCard(survey: SurveyEntity, onImageClick: (String) -> Unit) {
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

            if (survey.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = survey.thumbnailUrl,
                    contentDescription = survey.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { onImageClick(survey.imageUrl) },
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
