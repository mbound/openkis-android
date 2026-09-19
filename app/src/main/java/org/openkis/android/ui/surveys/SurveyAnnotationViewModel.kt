package org.openkis.android.ui.surveys

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openkis.android.data.export.SurveyAnnotationExporter
import org.openkis.android.data.local.dao.SurveyAnnotationDao
import org.openkis.android.data.local.entity.SurveyAnnotationEntity
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.local.entity.annotationKey
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SurveyAnnotationViewModel @Inject constructor(
    private val annotationDao: SurveyAnnotationDao,
    private val exporter: SurveyAnnotationExporter
) : ViewModel() {

    fun observe(survey: SurveyEntity): Flow<List<SurveyAnnotationEntity>> =
        annotationDao.observeBySurvey(
            serverUrl = survey.serverUrl,
            entityType = survey.entityType,
            dbId = survey.dbId,
            surveyKey = survey.annotationKey()
        )

    fun save(annotation: SurveyAnnotationEntity) {
        viewModelScope.launch {
            annotationDao.upsert(
                annotation.copy(
                    normalizedX = annotation.normalizedX.coerceIn(0f, 1f),
                    normalizedY = annotation.normalizedY.coerceIn(0f, 1f),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun delete(annotation: SurveyAnnotationEntity) {
        viewModelScope.launch {
            annotationDao.delete(annotation)
        }
    }

    suspend fun exportAnnotatedPng(
        context: Context,
        uri: Uri,
        file: File,
        annotations: List<SurveyAnnotationEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                exporter.exportAnnotatedPng(it, file, annotations)
            } ?: error("Could not open destination")
        }.isSuccess
    }

    suspend fun exportAnnotatedPdf(
        context: Context,
        uri: Uri,
        file: File,
        annotations: List<SurveyAnnotationEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                exporter.exportAnnotatedPdf(it, file, annotations)
            } ?: error("Could not open destination")
        }.isSuccess
    }
}
