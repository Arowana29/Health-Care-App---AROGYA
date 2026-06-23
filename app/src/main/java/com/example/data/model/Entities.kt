package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class PatientProfile(
    @PrimaryKey val id: String = "1",
    val name: String = "",
    val bloodGroup: String = "",
    val age: String = "",
    val gender: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val language: String = "English", // English, Sinhala, Tamil
    val location: String = "", // e.g. "Colombo", "Kandy"
    val pin: String = "",
    val photoUri: String? = null
)

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val diagnosis: String,
    val date: String,
    val doctorName: String,
    val notes: String = "",
    val profileId: String = "1"
)

@Entity(tableName = "documents")
data class MedicalDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // Lab Report, Prescription, X-Ray, Scan, Other
    val date: String,
    val providerName: String,
    val uri: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val imageBlob: ByteArray? = null,
    val analysis: String? = null,
    val profileId: String = "1"
)

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dose: String,
    val frequency: String,
    val doctorName: String,
    val status: String, // Active, Completed, Stopped
    val startDate: String = "",
    val endDate: String = "",
    val reminderTime: String = "09:00 AM",
    val profileId: String = "1",
    val frequencyType: String = "Daily", // "Daily", "Weekly"
    val frequencyDaysOfWeek: String = "", // comma-separated days, eg "Mon,Wed,Fri" or "1,3,5"
    val notificationEnabled: Boolean = true
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val logDate: String, // Format: yyyy-MM-dd
    val logTime: String, // Format: hh:mm a
    val status: String, // Taken, Missed, Skipped
    val notes: String = "",
    val profileId: String = "1"
)

@Entity(tableName = "visits")
data class DoctorVisit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val doctorName: String,
    val speciality: String,
    val hospital: String,
    val date: String,
    val reason: String,
    val notes: String,
    val linkedDocumentIds: String = "",
    val profileId: String = "1"
)

@Entity(tableName = "vital_signs")
data class VitalSign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // ISO format preferred for sorting
    val type: String, // "Blood Pressure" or "Blood Sugar"
    val value1: Int, // Systolic or Sugar level
    val value2: Int = 0, // Diastolic (0 for Sugar)
    val profileId: String = "1"
)

@Entity(tableName = "daily_vitals")
data class DailyVitals(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val profileId: String = "1"
)

@Entity(tableName = "health_expenses")
data class HealthExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: String, // yyyy-MM-dd
    val category: String, // Doctor Visit, Medicines, Lab Report, Other
    val profileName: String = "Janaka (Me)",
    val profileId: String = "1"
)

@Entity(tableName = "symptom_logs")
data class SymptomLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val severity: Int,
    val date: String, // yyyy-MM-dd
    val status: String, // Active, Resolved
    val notes: String = "",
    val profileId: String = "1"
)

@Entity(tableName = "saved_pharmacies")
data class SavedPharmacy(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String,
    val lat: Double?,
    val lon: Double?,
    val tagsJson: String?,
    val town: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

