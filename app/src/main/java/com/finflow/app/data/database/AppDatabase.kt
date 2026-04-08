package com.finflow.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.finflow.app.data.dao.CategoryDao
import com.finflow.app.data.dao.ExpenseDao
import com.finflow.app.data.entity.Category
import com.finflow.app.data.entity.Expense
import com.finflow.app.data.utils.DateConverter

@Database(
    entities = [Expense::class, Category::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finflow_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}