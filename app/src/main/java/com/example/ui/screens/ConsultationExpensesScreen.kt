package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.AppTitleText
import com.example.ui.components.TrilingualText
import com.example.ui.theme.*
import com.example.data.model.HealthExpense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationExpensesScreen(navController: NavController, viewModel: HealthViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val context = LocalContext.current

    // UI Local State
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMonthFilter by remember { mutableStateOf("All") }
    var selectedProfileFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Dialog state
    var descInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Doctor") } // Doctor, Medicine, Other
    var profileInput by remember { mutableStateOf("Janaka (Me)") }
    var dateInput by remember { mutableStateOf("") }

    // Initialize dateInput with today's date
    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            dateInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            // Clear inputs
            descInput = ""
            amountInput = ""
            categoryInput = "Doctor"
            profileInput = if (familyMembers.isNotEmpty()) familyMembers.first().name else "Janaka (Me)"
        }
    }

    // Parse all unique Month-Year strings for filtering
    val calendar = Calendar.getInstance()
    val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.US) // e.g. "June 2026"

    val uniqueMonths = remember(expenses) {
        val monthsSet = mutableSetOf<String>()
        expenses.forEach { exp ->
            try {
                val date = sdfSource.parse(exp.date)
                if (date != null) {
                    monthsSet.add(sdfMonthYear.format(date))
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        val sortedList = monthsSet.toList().sortedWith { m1, m2 ->
            try {
                val d1 = sdfMonthYear.parse(m1)
                val d2 = sdfMonthYear.parse(m2)
                d2.compareTo(d1) // descending order of months
            } catch (e: Exception) {
                0
            }
        }
        listOf("All") + sortedList
    }

    // Set default filter to most recent month if "All" is not preferred initially, 
    // or keep "All" to show all historical logs. Let's make "All" the default, but let users swipe easily.
    
    // Filtered Expenses
    val filteredExpenses = remember(expenses, selectedMonthFilter, selectedProfileFilter, selectedCategoryFilter) {
        expenses.filter { exp ->
            val matchesProfile = selectedProfileFilter == "All" || exp.profileName == selectedProfileFilter
            val matchesCategory = selectedCategoryFilter == "All" || exp.category == selectedCategoryFilter
            
            val matchesMonth = if (selectedMonthFilter == "All") {
                true
            } else {
                try {
                    val date = sdfSource.parse(exp.date)
                    if (date != null) {
                        sdfMonthYear.format(date) == selectedMonthFilter
                    } else false
                } catch (e: Exception) {
                    false
                }
            }
            matchesProfile && matchesCategory && matchesMonth
        }
    }

    // Metrics & breakdown calculations based on current filters
    val totalExpense = filteredExpenses.sumOf { it.amount }
    val doctorSpent = filteredExpenses.filter { it.category == "Doctor" }.sumOf { it.amount }
    val medicineSpent = filteredExpenses.filter { it.category == "Medicine" }.sumOf { it.amount }
    val otherSpent = filteredExpenses.filter { it.category != "Doctor" && it.category != "Medicine" }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Med-Expenses", "වෛද්‍ය වියදම්", "மருத்துவ செலவுகள்", color = White, scale = 0.95f)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryDarkTeal,
                contentColor = White,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Filled.Add, "Log Cost")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(LightGrayBackground)
        ) {
            // 1. MONTHLY SPENDING SUMMARY HEADER W/ BREAKDOWN
            Card(
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            TrilingualText(
                                english = "Total Medical Outlay",
                                sinhala = "මුළු වෛද්‍ය වියදම",
                                tamil = "மொத்த மருத்துவச் செலவு",
                                color = GrayText,
                                scale = 0.85f
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "LKR ${String.format("%,.2f", totalExpense)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ErrorRed
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(PastelPink, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedMonthFilter == "All") "Lifetime" else selectedMonthFilter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Dynamic Custom Category Distribution Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        val totalSum = doctorSpent + medicineSpent + otherSpent
                        if (totalSum > 0) {
                            val docWeight = (doctorSpent / totalSum).toFloat()
                            val medWeight = (medicineSpent / totalSum).toFloat()
                            val otherWeight = (otherSpent / totalSum).toFloat()

                            if (docWeight > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(docWeight)
                                        .background(PrimaryDarkTeal)
                                )
                            }
                            if (medWeight > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(medWeight)
                                        .background(IconOrange)
                                )
                            }
                            if (otherWeight > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(otherWeight)
                                        .background(IconPurple)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Breakdown legends with values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Category 1: Doctor Visits
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(PrimaryDarkTeal, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                TrilingualText("Doctor Visit", "වෛද්‍යවරයා", "வைத்தியர்", color = Black, scale = 0.75f)
                                Text("LKR ${String.format("%,.0f", doctorSpent)}", fontSize = 11.sp, color = GrayText)
                            }
                        }

                        // Category 2: Medicines
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(IconOrange, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                TrilingualText("Medicines", "ඖෂධ වර්ග", "மருந்து", color = Black, scale = 0.75f)
                                Text("LKR ${String.format("%,.0f", medicineSpent)}", fontSize = 11.sp, color = GrayText)
                            }
                        }

                        // Category 3: Other / Lab
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(IconPurple, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                TrilingualText("Other/Lab", "වෙනත්", "இதர", color = Black, scale = 0.75f)
                                Text("LKR ${String.format("%,.0f", otherSpent)}", fontSize = 11.sp, color = GrayText)
                            }
                        }
                    }
                }
            }

            // 2. HORIZONTAL SCROLL FILTERS (MONTH, PROFILE, CATEGORY)
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Month Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uniqueMonths.forEach { m ->
                        FilterChip(
                            selected = selectedMonthFilter == m,
                            onClick = { selectedMonthFilter = m },
                            label = { Text(m) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryDarkTeal,
                                selectedLabelColor = White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Profile Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterProfiles = listOf("All") + familyMembers.map { it.name }
                    filterProfiles.forEach { prof ->
                        FilterChip(
                            selected = selectedProfileFilter == prof,
                            onClick = { selectedProfileFilter = prof },
                            label = { Text(prof) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BackgroundLightTeal,
                                selectedLabelColor = PrimaryDarkTeal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. EXPENSE LIST
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Receipt,
                            contentDescription = "No Expenses",
                            tint = PrimaryDarkTeal.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TrilingualText(
                            english = "No expenses match your parameters.",
                            sinhala = "පරාමිතීන්ට ගැලපෙන වියදම් නොමැත.",
                            tamil = "தரவுகளுடன் பொருந்தக்கூடிய செலவுகள் இல்லை.",
                            color = GrayText,
                            scale = 0.85f,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses) { exp ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom visual asset indicator for categories
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = when (exp.category) {
                                                    "Doctor" -> PrimaryDarkTeal.copy(alpha = 0.1f)
                                                    "Medicine" -> IconOrange.copy(alpha = 0.1f)
                                                    else -> IconPurple.copy(alpha = 0.1f)
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (exp.category) {
                                                "Doctor" -> Icons.Filled.Person
                                                "Medicine" -> Icons.Filled.MedicalServices
                                                else -> Icons.Filled.Receipt
                                            },
                                            contentDescription = exp.category,
                                            tint = when (exp.category) {
                                                "Doctor" -> PrimaryDarkTeal
                                                "Medicine" -> IconOrange
                                                else -> IconPurple
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exp.description,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Black
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${exp.profileName} • ${exp.date}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LKR ${String.format("%,.0f", exp.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = ErrorRed,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteExpense(exp)
                                            Toast.makeText(context, "Log removed successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. ADD EXPENSE FLOATING DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                TrilingualText(
                    english = "Log New Expense",
                    sinhala = "වියදමක් එක් කරන්න",
                    tamil = "செலவைப் பதிவிடவும்",
                    color = PrimaryDarkTeal,
                    scale = 1.1f
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Description
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description (e.g. Dr. Visit, Amox)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_expense_desc")
                    )

                    // Amount
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (LKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_expense_amount")
                    )

                    // Category Toggle (Doctor Visit, Medicines, Other)
                    Text("Category / වර්ගීකරණය", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Doctor", "Medicine", "Other").forEach { catName ->
                            val isSelected = categoryInput == catName
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryDarkTeal else White
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { categoryInput = catName }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = catName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) White else GrayText
                                    )
                                }
                            }
                        }
                    }

                    // Member Selection
                    Text("Patient / රෝගියා", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val membersList = if (familyMembers.isEmpty()) listOf("Janaka (Me)") else familyMembers.map { it.name }
                        membersList.forEach { name ->
                            val isSelected = profileInput == name
                            FilterChip(
                                selected = isSelected,
                                onClick = { profileInput = name },
                                label = { Text(name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BackgroundLightTeal,
                                    selectedLabelColor = PrimaryDarkTeal
                                )
                            )
                        }
                    }

                    // Date Input
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_expense_date")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (descInput.isNotEmpty() && amount != null && amount > 0) {
                            viewModel.addExpenseWithDate(
                                description = descInput,
                                amount = amount,
                                category = categoryInput,
                                profileName = profileInput,
                                date = dateInput
                            )
                            showAddDialog = false
                            Toast.makeText(context, "Expense logged persistently", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please fill description and valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                    modifier = Modifier.testTag("add_expense_submit")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
