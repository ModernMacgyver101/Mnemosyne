package com.hardlinecoding.checkthebox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hardlinecoding.checkthebox.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Reminder::class, ToDoItem::class, LoggedEvent::class, Note::class], version = 11, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun toDoItemDao(): ToDoItemDao
    abstract fun loggedEventDao(): LoggedEventDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        private val _refreshSignal = MutableStateFlow(0)
        val refreshSignal: StateFlow<Int> = _refreshSignal.asStateFlow()

        fun triggerRefresh() {
            _refreshSignal.value += 1
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                // Initialize SQLCipher for 16KB support
                System.loadLibrary("sqlcipher")
                
                val builder = Room.databaseBuilder(context, AppDatabase::class.java, "mnemosyne_database")
                
                // Use a fixed internal key to ensure data is encrypted at rest.
                // This makes the database file unreadable by external file managers or local storage explorers.
                val factory = SupportOpenHelperFactory("Mnemosyne_Internal_Secure_Key_2024".toByteArray())
                builder.openHelperFactory(factory)
                
                builder.fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }

        fun closeDatabase() {
            Instance?.close()
            Instance = null
        }
    }
}
