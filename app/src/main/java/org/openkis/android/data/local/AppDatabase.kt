package org.openkis.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.openkis.android.data.local.dao.ArtificialDao
import org.openkis.android.data.local.dao.CaveDao
import org.openkis.android.data.local.dao.ServerDao
import org.openkis.android.data.local.dao.SpringDao
import org.openkis.android.data.local.dao.SurveyDao
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.ServerEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.local.entity.SurveyEntity

@Database(
    entities = [
        CaveEntity::class,
        SpringEntity::class,
        ArtificialEntity::class,
        ServerEntity::class,
        SurveyEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caveDao(): CaveDao
    abstract fun springDao(): SpringDao
    abstract fun artificialDao(): ArtificialDao
    abstract fun serverDao(): ServerDao
    abstract fun surveyDao(): SurveyDao
}
