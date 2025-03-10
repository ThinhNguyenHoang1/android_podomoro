package com.example.poromodo.model
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
        return formatter.parse(serialized) as ZonedDateTime
    }
}

data class Usage (
    val totalPodomoroTime: Long,
    val totalPodomoroReps: Long,
    val totalTasksCompleted: Long
)

@Entity
data class Task (
    @PrimaryKey val taskId: Long,
    val title: String,
    val description: String,
    val numOfPodomoroToComplete: Long,
    val numOfPodomoroSpend: Long,
    val totalTimeSpent: Long,
    val createdAt: ZonedDateTime,
)

@Entity(primaryKeys = ["taskId", "sessionId"])
data class TaskSessionCrossRef(
    val taskId: Long,
    val sessionId: Long
)

// Session: (Start) From opening app => Start at least one podomoro => Leave App (End)
@Entity
data class Session (
    @PrimaryKey val sessionId: Long,
    val timeFocus: Long,
    val timeBreak: Long,
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
abstract class AppDatabase: RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
}