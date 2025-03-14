package com.example.poromodo.model

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    @TypeConverter
    fun toString(entity: ZonedDateTime): String {
        return formatter.format(entity)
    }

    @TypeConverter
    fun fromString(serialized: String): ZonedDateTime {
        return try {
            ZonedDateTime.from(formatter.parse(serialized))
        } catch (e: Exception) {
            ZonedDateTime.now()
        }
    }
}

data class Usage(
    val totalPodomoroTime: Int,
    val totalPodomoroReps: Int,
    val totalTasksCompleted: Int
)

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Long = 0,
    val title: String,
    val description: String,
    val numOfPodomoroToComplete: Int = 0,
    val numOfPodomoroSpend: Int = 0,
    val totalTimeSpent: Int,
    val createdAt: ZonedDateTime,
)

@Entity(primaryKeys = ["taskId", "sessionId"])
data class TaskSessionCrossRef(
    val taskId: Long,
    val sessionId: Long
)

// Session: (Start) From opening app => Start at least one podomoro => Leave App (End)
@Entity
data class Session(
    @PrimaryKey(autoGenerate = true) val sessionId: Long,
    val timeFocus: Int,
    val timeBreak: Int,
    val startAt: ZonedDateTime,
    val endAt: ZonedDateTime
)

data class TaskWithSessions(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "taskId",
        entityColumn = "sessionId",
        associateBy = Junction(TaskSessionCrossRef::class)
    )
    val sessions: List<Session>
)


@Dao
interface TaskDao {
    @Upsert
    suspend fun upsertTask(contact: Task)

    @Delete
    suspend fun deleteTask(contact: Task)

    @Query("SELECT * FROM task ORDER BY createdAt DESC")
    fun getTasksOrderByCreatedDate(): Flow<List<Task>>

    @Transaction
    @Query("SELECT * FROM task")
    fun getTasksWithSessions(): List<TaskWithSessions>
}

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsertSession(contact: Session)

    @Delete
    suspend fun deleteSession(contact: Session)

    @Query("SELECT * FROM session ORDER BY startAt DESC")
    fun getSessionsByStartAt(): Flow<List<Session>>
}

@Database(
    entities = [Task::class, Session::class, TaskSessionCrossRef::class],
    version = 1
)
@TypeConverters(
    value = [ZonedDateTimeConverter::class]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}