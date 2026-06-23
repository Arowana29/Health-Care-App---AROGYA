package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.HealthWalletDao
import com.example.data.model.DailyVitals
import com.example.data.model.DoctorVisit
import com.example.data.model.MedicalDocument
import com.example.data.model.Medication
import com.example.data.model.PatientProfile
import com.example.data.model.VitalSign

import com.example.data.model.MedicationLog

import com.example.data.model.HealthExpense
import com.example.data.model.SymptomLog
import com.example.data.model.SavedPharmacy

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profile ADD COLUMN location TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_pharmacies` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `lat` REAL, `lon` REAL, `tagsJson` TEXT, `town` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profile ADD COLUMN photoUri TEXT")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE medications ADD COLUMN frequencyType TEXT NOT NULL DEFAULT 'Daily'")
        db.execSQL("ALTER TABLE medications ADD COLUMN frequencyDaysOfWeek TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE medications ADD COLUMN notificationEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `medical_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `diagnosis` TEXT NOT NULL, `date` TEXT NOT NULL, `doctorName` TEXT NOT NULL, `notes` TEXT NOT NULL, `profileId` TEXT NOT NULL)")
    }
}

@Database(entities = [PatientProfile::class, MedicalDocument::class, Medication::class, DoctorVisit::class, VitalSign::class, DailyVitals::class, MedicationLog::class, HealthExpense::class, SymptomLog::class, SavedPharmacy::class, com.example.data.model.MedicalRecord::class], version = 16, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthWalletDao(): HealthWalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_wallet_database"
                )
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
