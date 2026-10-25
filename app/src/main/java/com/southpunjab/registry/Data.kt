package com.southpunjab.registry
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "students", indices = [Index("admNo", unique = true)])
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admNo: String, val name: String, val father: String, val dob: String, val gender: String,
    val cls: String, val admDate: String, val photo: String = "", val phone: String, val cnic: String = "",
    val address: String, val prevSchool: String = "", val remarks: String = "", val demo: Boolean = false)

@Dao interface StudentDao {
    @Query("SELECT * FROM students") fun all(): Flow<List<Student>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(s: Student): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(l: List<Student>)
    @Update suspend fun update(s: Student)
    @Delete suspend fun delete(s: Student)
    @Query("DELETE FROM students WHERE demo = 1") suspend fun clearDemo()
}
@Database(entities = [Student::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() { abstract fun dao(): StudentDao }
