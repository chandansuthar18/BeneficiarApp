package com.example.beneficiaryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.presentation.theme.TopBar
import com.example.beneficiaryapp.presentation.theme.primaryGreen
import com.example.beneficiaryapp.utils.NetworkMonitor
import com.example.beneficiaryapp.viewmodels.BeneficiaryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyScreen(navController: NavHostController? = null) {
    val viewModel: BeneficiaryViewModel = hiltViewModel()
    val pregnancyWeeks = (1..40).map { "$it weeks" }
    val gravidaOptions = (1..10).map { it.toString() }
    val paraOptions = (0..10).map { it.toString() }

    var selectedWeek by remember { mutableStateOf("") }
    var selectedGravida by remember { mutableStateOf("") }
    var selectedPara by remember { mutableStateOf("") }

    var weekExpanded by remember { mutableStateOf(false) }
    var gravidaExpanded by remember { mutableStateOf(false) }
    var paraExpanded by remember { mutableStateOf(false) }

    // State for UI feedback
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Network monitoring
    val networkMonitor = NetworkMonitor(context)
    val isOnline by networkMonitor.isOnline.collectAsState(initial = false)

    // Validate form
    val isFormValid = selectedWeek.isNotEmpty() && selectedGravida.isNotEmpty() && selectedPara.isNotEmpty()

    // ✅ FIXED: Load basic info when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadBasicInfoFromStorage()
    }

    // Observe save result
    LaunchedEffect(viewModel.saveResult.value) {
        viewModel.saveResult.value?.let { result ->
            if (result.isSuccess) {
                val message = result.getOrNull() ?: "Data saved successfully"
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long
                    )

                    // Navigate back after success
                    kotlinx.coroutines.delay(2000)
                    navController?.popBackStack()
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to save data"
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "❌ Error: $error",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar with Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(TopBar)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Back Button on left side with padding
                IconButton(
                    onClick = {
                        navController?.popBackStack()
                    },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Centered Title - takes remaining space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pregnancy Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                }
            }
        }

        // Network Status Indicator
        if (!isOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Yellow.copy(alpha = 0.9f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You are offline. Data will be saved locally.",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Scrollable Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Pregnancy Details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp
                )
            }

            // Week of Pregnancy Dropdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Current Week of Pregnancy from dropdown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = weekExpanded,
                    onExpandedChange = { weekExpanded = !weekExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedWeek,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = {
                            Text(
                                "14 weeks",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = weekExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = weekExpanded,
                        onDismissRequest = { weekExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        pregnancyWeeks.forEach { week ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        week,
                                        color = Color.Black,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    selectedWeek = week
                                    weekExpanded = false
                                    // Update viewModel
                                    viewModel.pregnancyWeek.value = week
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Obstetric History Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Obstetric History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp
                )
            }

            // Gravida Field (full width)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Gravida (total pregnancies)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = gravidaExpanded,
                    onExpandedChange = { gravidaExpanded = !gravidaExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGravida,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = {
                            Text(
                                "1",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = gravidaExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = gravidaExpanded,
                        onDismissRequest = { gravidaExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        gravidaOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        color = Color.Black,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    selectedGravida = option
                                    gravidaExpanded = false
                                    // Update viewModel
                                    viewModel.gravida.value = option
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Para Field (full width - below Gravida)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Para (number of live births)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = paraExpanded,
                    onExpandedChange = { paraExpanded = !paraExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPara,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = {
                            Text(
                                "0",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = paraExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = paraExpanded,
                        onDismissRequest = { paraExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        paraOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        color = Color.Black,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    selectedPara = option
                                    paraExpanded = false
                                    // Update viewModel
                                    viewModel.para.value = option
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Empty space to push button to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Submit Button at the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        // Ensure beneficiary status is set to PREGNANT
                        if (viewModel.beneficiaryStatus.value != BeneficiaryStatus.PREGNANT) {
                            viewModel.setBeneficiaryStatus(BeneficiaryStatus.PREGNANT)
                        }

                        // Save complete beneficiary data (offline-first)
                        viewModel.saveCompleteBeneficiaryData(
                            isPregnancy = true
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                        disabledContentColor = Color.White
                    ),
                    enabled = isFormValid && !viewModel.isLoading.value
                ) {
                    if (viewModel.isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "Submit Pregnancy Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Show validation error
                if (!isFormValid && (selectedWeek.isNotEmpty() || selectedGravida.isNotEmpty() || selectedPara.isNotEmpty())) {
                    Text(
                        text = "Please fill all pregnancy details",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Snackbar Host
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )
}