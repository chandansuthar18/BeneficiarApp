package com.example.beneficiaryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.beneficiaryapp.presentation.theme.TopBar
import com.example.beneficiaryapp.viewmodels.EditBeneficiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBeneficiaryScreen(
    navController: NavHostController,
    beneficiaryId: String,
    viewModel: EditBeneficiaryViewModel = hiltViewModel()
) {
    // Initialize view model with beneficiary ID
    LaunchedEffect(beneficiaryId) {
        viewModel.loadBeneficiary(beneficiaryId)
    }

    val beneficiaryState by viewModel.beneficiaryState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Handle navigation after successful save or delete
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            // Navigate back after a short delay
            kotlinx.coroutines.delay(1000)
            navController.popBackStack()
        }
    }

    // Handle delete success
    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            // Already handled by successMessage flow
        }
    }

    Scaffold(
        topBar = {
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
                    // Back Button
                    IconButton(
                        onClick = { navController.popBackStack() },
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

                    // Title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit Beneficiary",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (beneficiaryState == null || beneficiaryState?.id.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Beneficiary not found",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            val beneficiary = beneficiaryState!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Basic Information Section
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Name Field
                OutlinedTextField(
                    value = beneficiary.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = beneficiary.name.isEmpty()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Age Field
                OutlinedTextField(
                    value = beneficiary.age,
                    onValueChange = { viewModel.updateAge(it) },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = beneficiary.age.isEmpty()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CNIC Field
                OutlinedTextField(
                    value = beneficiary.cnic,
                    onValueChange = { viewModel.updateCNIC(it) },
                    label = { Text("CNIC") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = beneficiary.cnic.isEmpty()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Field
                OutlinedTextField(
                    value = beneficiary.phoneNumber,
                    onValueChange = { viewModel.updatePhoneNumber(it) },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = beneficiary.phoneNumber.isEmpty()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Address Information Section
                Text(
                    text = "Address Information",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Temporary Address
                OutlinedTextField(
                    value = beneficiary.temporaryAddress,
                    onValueChange = { viewModel.updateTemporaryAddress(it) },
                    label = { Text("Temporary Address") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = beneficiary.temporaryAddress.isEmpty()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permanent Address
                OutlinedTextField(
                    value = beneficiary.permanentAddress,
                    onValueChange = { viewModel.updatePermanentAddress(it) },
                    label = { Text("Permanent Address") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = beneficiary.permanentAddress.isEmpty()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Document Information Section
                Text(
                    text = "Document Information",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Issue Date
                OutlinedTextField(
                    value = beneficiary.issueDate,
                    onValueChange = { viewModel.updateIssueDate(it) },
                    label = { Text("Issue Date (dd-MMM-yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = beneficiary.issueDate.isEmpty()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Expire Date
                OutlinedTextField(
                    value = beneficiary.expireDate,
                    onValueChange = { viewModel.updateExpireDate(it) },
                    label = { Text("Expire Date (dd-MMM-yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = beneficiary.expireDate.isEmpty()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delete Button
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveChanges() },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Beneficiary") },
            text = { Text("Are you sure you want to delete this beneficiary? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBeneficiary()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Snackbar
    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            viewModel.clearErrorMessage()
        }

        Snackbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = errorMessage!!)
            }
        }
    }

    // Success Snackbar
    if (successMessage != null) {
        LaunchedEffect(successMessage) {
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(1000)
            viewModel.clearSuccessMessage()
        }

        Snackbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = successMessage!!)
            }
        }
    }
}