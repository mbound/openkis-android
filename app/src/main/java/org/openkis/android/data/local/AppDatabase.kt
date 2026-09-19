package org.openkis.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.openkis.android.data.local.dao.ArtificialDao
import org.openkis.android.data.local.dao.CaveDao
import org.openkis.android.data.local.dao.ServerDao
import org.openkis.android.data.local.dao.SpringDao
import org.openkis.android.data.local.dao.SurveyDao
import org.openkis.android.data.local.dao.SurveyAnnotationDao
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.ServerEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.local.entity.SurveyAnnotationEntity

@Database(
    entities = [
        CaveEntity::class,
        SpringEntity::class,
        ArtificialEntity::class,
        ServerEntity::class,
        SurveyEntity::class,
        SurveyAnnotationEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caveDao(): CaveDao
    abstract fun springDao(): SpringDao
    abstract fun artificialDao(): ArtificialDao
    abstract fun serverDao(): ServerDao
    abstract fun surveyDao(): SurveyDao
    abstract fun surveyAnnotationDao(): SurveyAnnotationDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `survey_annotations` (
                        `id` TEXT NOT NULL,
                        `serverUrl` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `dbId` TEXT NOT NULL,
                        `surveyKey` TEXT NOT NULL,
                        `normalizedX` REAL NOT NULL,
                        `normalizedY` REAL NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `metadata` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_survey_annotations_serverUrl_entityType_dbId_surveyKey`
                    ON `survey_annotations` (`serverUrl`, `entityType`, `dbId`, `surveyKey`)
                    """.trimIndent()
                )
            }
        }
    }
}
