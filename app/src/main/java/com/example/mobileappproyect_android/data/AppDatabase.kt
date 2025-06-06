package com.example.mobileappproyect_android.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SubscriptionEntity::class], version = 1, exportSchema = false) // Incrementa la versión en migraciones
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile // Asegura que el valor de INSTANCE sea siempre actualizado y visible para todos los hilos
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d("AppDatabase", "getDatabase called")
            return INSTANCE ?: synchronized(this) { // Bloque sincronizado para creación segura en múltiples hilos
                Log.d("AppDatabase", "INSTANCE is null, creating new database instance.")
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "subscription_database" // Nombre del archivo de la base de datos
                    )
                        .fallbackToDestructiveMigration()
                        // .fallbackToDestructiveMigration() // Opcional: Para desarrollo, si no quieres definir migraciones
                        .build()
                    Log.d("AppDatabase", "Database instance CREATED and BUILT successfully.")
                    INSTANCE = instance
                    instance
                }catch (e: Exception) {
                    Log.e("AppDatabase", "ERROR CREATING DATABASE INSTANCE", e) // CRITICAL LOG
                    throw e // Re-throw the exception so you see it clearly as the cause
                }
            }
        }
    }
}