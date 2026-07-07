package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DoctorVisit
import com.example.data.model.MedicalDocument
import com.example.data.model.Medication
import com.example.data.model.MedicalRecord
import com.example.data.model.PatientProfile
import com.example.data.model.VitalSign
import com.example.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthWalletDao {
    @Query("SELECT * FROM profile WHERE id = '1'")
    fun getProfile(): Flow<PatientProfile?>

    @Query("SELECT * FROM profile")
    fun getAllProfiles(): Flow<List<PatientProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PatientProfile)

    @Query("SELECT * FROM documents ORDER BY id DESC")
    fun getAllDocuments(): Flow<List<MedicalDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MedicalDocument)

    @androidx.room.Delete
    suspend fun deleteDocument(document: MedicalDocument)

    @Query("SELECT * FROM medications ORDER BY status ASC, id DESC")
    fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @androidx.room.Delete
    suspend fun deleteMedication(medication: Medication)

    @androidx.room.Update
    suspend fun updateMedication(medication: Medication)

    @Query("SELECT * FROM medication_logs ORDER BY logDate DESC, id DESC")
    fun getAllMedicationLogs(): Flow<List<MedicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationLog(log: MedicationLog)

    @androidx.room.Delete
    suspend fun deleteMedicationLog(log: MedicationLog)

    @Query("SELECT * FROM visits ORDER BY id DESC")
    fun getAllVisits(): Flow<List<DoctorVisit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: DoctorVisit): Long

    @androidx.room.Delete
    suspend fun deleteVisit(visit: DoctorVisit)

    @Query("SELECT * FROM vital_signs WHERE type = :type ORDER BY date ASC")
    fun getVitalSignsByType(type: String): Flow<List<VitalSign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitalSigns(vitalSigns: List<VitalSign>)

    @Query("SELECT * FROM daily_vitals ORDER BY date ASC")
    fun getAllDailyVitals(): Flow<List<com.example.data.model.DailyVitals>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyVitals(dailyVitals: com.example.data.model.DailyVitals)

    @Query("SELECT * FROM health_expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<com.example.data.model.HealthExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: com.example.data.model.HealthExpense)

    @androidx.room.Delete
    suspend fun deleteExpense(expense: com.example.data.model.HealthExpense)

    @Query("SELECT * FROM symptom_logs ORDER BY date DESC, id DESC")
    fun getAllSymptomLogs(): Flow<List<com.example.data.model.SymptomLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomLog(symptomLog: com.example.data.model.SymptomLog)

    @androidx.room.Delete
    suspend fun deleteSymptomLog(symptomLog: com.example.data.model.SymptomLog)

    @Query("SELECT * FROM saved_pharmacies ORDER BY timestamp DESC")
    fun getAllSavedPharmacies(): Flow<List<com.example.data.model.SavedPharmacy>>

    @Query("SELECT * FROM saved_pharmacies WHERE town LIKE '%' || :town || '%' ORDER BY timestamp DESC")
    suspend fun getSavedPharmaciesByTown(town: String): List<com.example.data.model.SavedPharmacy>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPharmacy(pharmacy: com.example.data.model.SavedPharmacy)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPharmacies(pharmacies: List<com.example.data.model.SavedPharmacy>)

    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    fun getAllMedicalRecords(): Flow<List<MedicalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecord)

    @androidx.room.Delete
    suspend fun deleteMedicalRecord(record: MedicalRecord)
}
