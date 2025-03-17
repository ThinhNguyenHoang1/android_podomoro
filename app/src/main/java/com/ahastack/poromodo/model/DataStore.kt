package com.ahastack.poromodo.model

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class PomodoroPhase { PODOMORO, BREAK, LONG_BREAK }

class ZonedDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    // DB: TEXT: 2025-03-15T16:23:23.422004+07:00[Asia/Ho_Chi_Minh] <-> ZoneDateTime
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

// TODO: Update time spent on task.
// TODO: Consider if we should isCompleted in here to implement sorting / filtering.
@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Long = 0,
    val title: String,
    val description: String,
    val numOfPodomoroToComplete: Int = 0,
    val numOfPodomoroSpend: Int = 0,
    val totalTimeSpent: Int,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
)

@Entity(primaryKeys = ["taskId", "sessionId"])
data class TaskSessionCrossRef(
    val taskId: Long,
    val sessionId: Long
)

// Session: (Start) From opening app => Start at least one podomoro => Leave App (End)
@Entity
data class Session(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val timeFocus: Int,
    val timeBreak: Int,
    val startAt: ZonedDateTime, // 2025-03-15T16:23:23.422004+07:00[Asia/Ho_Chi_Minh]
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
    suspend fun upsertTask(t: Task)

    @Update
    suspend fun updateTask(t: Task)

    @Delete
    suspend fun deleteTask(t: Task)

    @Query("SELECT * FROM task WHERE taskId=:id")
    fun getTaskById(id: Long): Flow<Task?>

    @Query("SELECT * FROM task ORDER BY createdAt DESC")
    fun getTasksOrderByCreatedDate(): Flow<List<Task>>

    @Transaction
    @Query("SELECT * FROM task")
    fun getTasksWithSessions(): List<TaskWithSessions>
}

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsertSession(s: Session)

    @Delete
    suspend fun deleteSession(s: Session)

    @Query("SELECT * FROM session ORDER BY startAt DESC")
    fun getSessionsByStartAt(): Flow<List<Session>>

    // Reports Data Queries
    // Select the dates in the week (Left outer join)
    // Join the date with the date from sessions
    // Group by the date
    // Sum the totalMinute
    @Query(
        """
select "MONDAY" as day ,COALESCE(sum(timeFocus), 0) as totalMinutes from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 0', '-7 days')
UNION
select "TUESDAY" as day, sum(timeFocus) as totalMinutes from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 1', '-7 days')
UNION
select "WEDNESDAY" as day ,COALESCE(sum(timeFocus), 0) as totalMinutes  from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 2', '-7 days')
UNION
select "THURSDAY" as day ,COALESCE(sum(timeFocus), 0) as totalMinutes  from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 3', '-7 days')
UNION
select "FRIDAY" as day ,COALESCE(sum(timeFocus), 0) as totalMinutes  from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 4', '-7 days')
UNION
select "SATURDAY" as day ,COALESCE(sum(timeFocus), 0) as totalMinutes  from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 5', '-7 days')
UNION
select "SUNDAY" as day ,COALESCE(sum(timeFocus), 0) as totalMinutes  from session where DATE(SUBSTRING(startAt,1 , 19)) == DATE('now', 'weekday 6', '-7 days')
        """
    )
    fun getWeeklyUsage(): List<WeeklyUsage>

    @Query(
        "SELECT AVG(totalMinutes) AS averageMinutes " +
                "FROM (SELECT strftime('%Y-%m-%d', startAt, 'localtime') AS day, SUM(timeFocus) AS totalMinutes " +
                "      FROM session " +
                "      WHERE datetime(startAt, 'localtime') >= date('now', '-7 days', 'localtime') " +
                "      GROUP BY strftime('%Y-%m-%d', startAt, 'localtime'))"
    )
    fun getAverageDailyUsage(): Float

    @Query(
        "SELECT COUNT(*) AS visitCount " +
                "FROM session " +
                "WHERE datetime(startAt, 'localtime') >= date('now', '-7 days', 'localtime')"
    )
    fun getTotalWeeklyVisits(): Int

    @Query(
        "SELECT AVG(visitCount) AS averageVisits " +
                "FROM (SELECT COUNT(*) AS visitCount " +
                "      FROM session " +
                "      WHERE datetime(startAt, 'localtime') >= date('now', '-7 days', 'localtime') " +
                "      GROUP BY strftime('%Y-%m-%d', startAt, 'localtime'))"
    )
    fun getAverageDailyVisits(): Float

    @Query(
        "SELECT COUNT(*) AS visitCount " +
                "FROM session " +
                "WHERE strftime('%Y-%m-%d', startAt, 'localtime') = date('now', 'localtime')"
    )
    fun getTodayVisits(): Int

    @Query(
        "SELECT SUM(timeFocus) AS totalMinutes " +
                "FROM session " +
                "WHERE strftime('%Y-%m-%d', startAt, 'localtime') = date('now', 'localtime')"
    )
    fun getTodayUsage(): Int

    @Query(
        "SELECT SUM(timeFocus) AS totalMinutes " +
                "FROM session " +
                "WHERE datetime(startAt, 'localtime') >= date('now', '-7 days', 'localtime')"
    )
    fun getWeeklyUsageTotal(): Int

    @Query(
        "SELECT SUM(timeFocus) AS totalMinutes " +
                "FROM session " +
                "WHERE strftime('%Y-%m', startAt, 'localtime') = strftime('%Y-%m', 'now', 'localtime')"
    )
    fun getMonthlyUsageTotal(): Int
}

data class WeeklyUsage(
    val day: String,
    val totalMinutes: Int
)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val dt = ZonedDateTime.now()
        val now = formatter.format(dt)
        db.execSQL("ALTER TABLE task ADD COLUMN updatedAt TEXT NOT NULL DEFAULT '$now'")
    }
}


@Database(
    entities = [Task::class, Session::class, TaskSessionCrossRef::class],
    version = 2
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also {
                        Instance = it
                    }
            }
        }
    }
}