package com.example.data

import com.example.data.dao.HealthWalletDao
import com.example.data.model.DoctorVisit
import com.example.data.model.MedicalDocument
import com.example.data.model.Medication
import com.example.data.model.PatientProfile
import com.example.data.model.VitalSign
import com.example.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

class HealthRepository(private val healthWalletDao: HealthWalletDao) {
    val profile: Flow<PatientProfile?> = healthWalletDao.getProfile()
    val allProfiles: Flow<List<PatientProfile>> = healthWalletDao.getAllProfiles()
    val documents: Flow<List<MedicalDocument>> = healthWalletDao.getAllDocuments()
    val medications: Flow<List<Medication>> = healthWalletDao.getAllMedications()
    val medicationLogs: Flow<List<MedicationLog>> = healthWalletDao.getAllMedicationLogs()
    val visits: Flow<List<DoctorVisit>> = healthWalletDao.getAllVisits()
    val dailyVitals: Flow<List<com.example.data.model.DailyVitals>> = healthWalletDao.getAllDailyVitals()
    val allExpenses: Flow<List<com.example.data.model.HealthExpense>> = healthWalletDao.getAllExpenses()
    val allSymptomLogs: Flow<List<com.example.data.model.SymptomLog>> = healthWalletDao.getAllSymptomLogs()
    val medicalRecords: Flow<List<com.example.data.model.MedicalRecord>> = healthWalletDao.getAllMedicalRecords()

    fun getVitalSignsByType(type: String): Flow<List<VitalSign>> {
        return healthWalletDao.getVitalSignsByType(type)
    }

    suspend fun insertVitalSigns(vitalSigns: List<VitalSign>) {
        healthWalletDao.insertVitalSigns(vitalSigns)
    }

    suspend fun saveProfile(profile: PatientProfile) {
        healthWalletDao.saveProfile(profile)
    }

    suspend fun insertDocument(document: MedicalDocument) {
        healthWalletDao.insertDocument(document)
    }

    suspend fun deleteDocument(document: MedicalDocument) {
        healthWalletDao.deleteDocument(document)
    }

    suspend fun insertMedication(medication: Medication): Long {
        return healthWalletDao.insertMedication(medication)
    }

    suspend fun updateMedication(medication: Medication) {
        healthWalletDao.updateMedication(medication)
    }

    suspend fun deleteMedication(medication: Medication) {
        healthWalletDao.deleteMedication(medication)
    }

    suspend fun insertMedicationLog(log: MedicationLog) {
        healthWalletDao.insertMedicationLog(log)
    }

    suspend fun deleteMedicationLog(log: MedicationLog) {
        healthWalletDao.deleteMedicationLog(log)
    }

    suspend fun insertVisit(visit: DoctorVisit) {
        healthWalletDao.insertVisit(visit)
    }

    suspend fun insertDailyVitals(vitals: com.example.data.model.DailyVitals) {
        healthWalletDao.insertDailyVitals(vitals)
    }

    suspend fun insertExpense(expense: com.example.data.model.HealthExpense) {
        healthWalletDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: com.example.data.model.HealthExpense) {
        healthWalletDao.deleteExpense(expense)
    }

    suspend fun insertSymptomLog(symptomLog: com.example.data.model.SymptomLog) {
        healthWalletDao.insertSymptomLog(symptomLog)
    }

    suspend fun deleteSymptomLog(symptomLog: com.example.data.model.SymptomLog) {
        healthWalletDao.deleteSymptomLog(symptomLog)
    }

    suspend fun getSavedPharmaciesByTown(town: String): List<com.example.data.model.SavedPharmacy> {
        return healthWalletDao.getSavedPharmaciesByTown(town)
    }

    suspend fun insertSavedPharmacies(pharmacies: List<com.example.data.model.SavedPharmacy>) {
        healthWalletDao.insertSavedPharmacies(pharmacies)
    }

    suspend fun insertMedicalRecord(record: com.example.data.model.MedicalRecord) {
        healthWalletDao.insertMedicalRecord(record)
    }

    suspend fun deleteMedicalRecord(record: com.example.data.model.MedicalRecord) {
        healthWalletDao.deleteMedicalRecord(record)
    }
}
