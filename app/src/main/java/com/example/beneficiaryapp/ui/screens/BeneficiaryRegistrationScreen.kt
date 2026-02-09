package com.example.beneficiaryapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.presentation.theme.TopBar
import com.example.beneficiaryapp.presentation.theme.primaryGreen
import com.example.beneficiaryapp.utils.NetworkMonitor
import com.example.beneficiaryapp.utils.ValidationUtils
import com.example.beneficiaryapp.viewmodels.BeneficiaryViewModel
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryRegistrationScreen(navController: NavHostController? = null) {
    val viewModel: BeneficiaryViewModel = hiltViewModel()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Date picker dialog states
    val dateOfBirthDialogState = rememberMaterialDialogState()
    val issueDateDialogState = rememberMaterialDialogState()
    val expireDateDialogState = rememberMaterialDialogState()

    // Validation states
    val nameError = remember { mutableStateOf<String?>(null) }
    val ageError = remember { mutableStateOf<String?>(null) }
    val cnicError = remember { mutableStateOf<String?>(null) }
    val dateOfBirthError = remember { mutableStateOf<String?>(null) }
    val genderError = remember { mutableStateOf<String?>(null) }
    val phoneError = remember { mutableStateOf<String?>(null) }
    val tempAddressError = remember { mutableStateOf<String?>(null) }
    val permAddressError = remember { mutableStateOf<String?>(null) }
    val districtError = remember { mutableStateOf<String?>(null) }
    val talukaError = remember { mutableStateOf<String?>(null) }
    val unionCouncilError = remember { mutableStateOf<String?>(null) }
    val issueDateError = remember { mutableStateOf<String?>(null) }
    val expireDateError = remember { mutableStateOf<String?>(null) }
    val dateRangeError = remember { mutableStateOf<String?>(null) }
    val beneficiaryStatusError = remember { mutableStateOf<String?>(null) }

    // Network monitoring
    val networkMonitor = NetworkMonitor(context)
    val isOnline by networkMonitor.isOnline.collectAsState(initial = false)

    // Loading state
    val isLoading by viewModel.isLoading

    // Form validation
    val isFormValid = derivedStateOf {
        nameError.value == null &&
                ageError.value == null &&
                cnicError.value == null &&
                dateOfBirthError.value == null &&
                genderError.value == null &&
                phoneError.value == null &&
                tempAddressError.value == null &&
                permAddressError.value == null &&
                districtError.value == null &&
                talukaError.value == null &&
                unionCouncilError.value == null &&
                issueDateError.value == null &&
                expireDateError.value == null &&
                dateRangeError.value == null &&
                beneficiaryStatusError.value == null &&
                viewModel.name.value.isNotBlank() &&
                viewModel.age.value.isNotBlank() &&
                viewModel.cnic.value.isNotBlank() &&
                viewModel.dateOfBirth.value.isNotBlank() &&
                viewModel.gender.value.isNotBlank() &&
                viewModel.phoneNumber.value.isNotBlank() &&
                viewModel.temporaryAddress.value.isNotBlank() &&
                viewModel.permanentAddress.value.isNotBlank() &&
                viewModel.district.value.isNotBlank() &&
                viewModel.taluka.value.isNotBlank() &&
                viewModel.unionCouncil.value.isNotBlank() &&
                viewModel.issueDate.value.isNotBlank() &&
                viewModel.expireDate.value.isNotBlank() &&
                viewModel.beneficiaryStatus.value != null
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
        // Top Bar - Green Header
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
                    onClick = {
                        navController?.navigate("login") {
                            popUpTo("registration") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardBackspace,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Centered Title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Verify Identity",
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
            // Personal Information Section
            PersonalInfoSection(
                name = viewModel.name.value,
                onNameChange = {
                    viewModel.name.value = it
                    nameError.value = ValidationUtils.getNameError(it)
                },
                nameError = nameError.value,
                age = viewModel.age.value,
                onAgeChange = {
                    viewModel.age.value = it
                    ageError.value = ValidationUtils.getAgeError(it)
                },
                ageError = ageError.value,
                cnic = viewModel.cnic.value,
                onCnicChange = {
                    viewModel.cnic.value = it
                    cnicError.value = ValidationUtils.getCNICError(it)
                },
                cnicError = cnicError.value,
                dateOfBirth = viewModel.dateOfBirth.value,
                onDateOfBirthClick = { dateOfBirthDialogState.show() },
                dateOfBirthError = dateOfBirthError.value,
                gender = viewModel.gender.value,
                onGenderChange = {
                    viewModel.gender.value = it
                    genderError.value = if (it.isEmpty()) "Gender is required" else null
                },
                genderError = genderError.value
            )

            // Contact Information Section
            ContactInfoSection(
                phoneNumber = viewModel.phoneNumber.value,
                onPhoneNumberChange = {
                    viewModel.phoneNumber.value = it
                    phoneError.value = ValidationUtils.getPhoneError(it)
                },
                phoneError = phoneError.value,
                temporaryAddress = viewModel.temporaryAddress.value,
                onTemporaryAddressChange = {
                    viewModel.temporaryAddress.value = it
                    tempAddressError.value = ValidationUtils.getAddressError(it, "Temporary address")
                },
                tempAddressError = tempAddressError.value,
                permanentAddress = viewModel.permanentAddress.value,
                onPermanentAddressChange = {
                    viewModel.permanentAddress.value = it
                    permAddressError.value = ValidationUtils.getAddressError(it, "Permanent address")
                },
                permAddressError = permAddressError.value
            )

            // Location Information Section
            LocationInfoSection(
                district = viewModel.district.value,
                onDistrictChange = {
                    viewModel.district.value = it
                    districtError.value = ValidationUtils.getLocationError(it, "District")
                },
                districtError = districtError.value,
                taluka = viewModel.taluka.value,
                onTalukaChange = {
                    viewModel.taluka.value = it
                    talukaError.value = ValidationUtils.getLocationError(it, "Taluka")
                },
                talukaError = talukaError.value,
                unionCouncil = viewModel.unionCouncil.value,
                onUnionCouncilChange = {
                    viewModel.unionCouncil.value = it
                    unionCouncilError.value = ValidationUtils.getLocationError(it, "Union Council")
                },
                unionCouncilError = unionCouncilError.value
            )

            // Document Information Section
            DocumentInfoSection(
                issueDate = viewModel.issueDate.value,
                onIssueDateClick = { issueDateDialogState.show() },
                issueDateError = issueDateError.value,
                expireDate = viewModel.expireDate.value,
                onExpireDateClick = { expireDateDialogState.show() },
                expireDateError = expireDateError.value,
                dateRangeError = dateRangeError.value
            )

            // Beneficiary Status Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Beneficiary Status",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                BeneficiaryStatusSection(
                    beneficiaryStatus = viewModel.beneficiaryStatus.value,
                    beneficiaryStatusError = beneficiaryStatusError.value,
                    onPregnantClick = {
                        viewModel.beneficiaryStatus.value = BeneficiaryStatus.PREGNANT
                        beneficiaryStatusError.value = null
                    },
                    onLactatingClick = {
                        viewModel.beneficiaryStatus.value = BeneficiaryStatus.LACTATING
                        beneficiaryStatusError.value = null
                    }
                )
            }

            // Submit Button - FIXED: Added saveBasicInfoToStorage()
            Button(
                onClick = {
                    // Validate all fields before submission
                    nameError.value = ValidationUtils.getNameError(viewModel.name.value)
                    ageError.value = ValidationUtils.getAgeError(viewModel.age.value)
                    cnicError.value = ValidationUtils.getCNICError(viewModel.cnic.value)
                    dateOfBirthError.value = ValidationUtils.getDateError(viewModel.dateOfBirth.value, "Date of Birth")
                    genderError.value = if (viewModel.gender.value.isEmpty()) "Gender is required" else null
                    phoneError.value = ValidationUtils.getPhoneError(viewModel.phoneNumber.value)
                    tempAddressError.value = ValidationUtils.getAddressError(viewModel.temporaryAddress.value, "Temporary address")
                    permAddressError.value = ValidationUtils.getAddressError(viewModel.permanentAddress.value, "Permanent address")
                    districtError.value = ValidationUtils.getLocationError(viewModel.district.value, "District")
                    talukaError.value = ValidationUtils.getLocationError(viewModel.taluka.value, "Taluka")
                    unionCouncilError.value = ValidationUtils.getLocationError(viewModel.unionCouncil.value, "Union Council")
                    issueDateError.value = ValidationUtils.getDateError(viewModel.issueDate.value, "Issue Date")
                    expireDateError.value = ValidationUtils.getDateError(viewModel.expireDate.value, "Expire Date")
                    dateRangeError.value = ValidationUtils.getDateRangeError(viewModel.issueDate.value, viewModel.expireDate.value)
                    beneficiaryStatusError.value = if (viewModel.beneficiaryStatus.value == null) "Please select beneficiary status" else null

                    if (isFormValid.value) {
                        // ✅ FIXED: Save basic info to storage BEFORE navigation
                        viewModel.saveBasicInfoToStorage()

                        // Navigate based on beneficiary status
                        when (viewModel.beneficiaryStatus.value) {
                            BeneficiaryStatus.PREGNANT -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Proceeding to pregnancy details...")
                                }
                                navController?.navigate("pregnancy")
                            }
                            BeneficiaryStatus.LACTATING -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Proceeding to lactating details...")
                                }
                                navController?.navigate("lactating")
                            }
                            else -> {
                                beneficiaryStatusError.value = "Please select beneficiary status"
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all required fields correctly")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                enabled = isFormValid.value && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = "Next: Add Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Snackbar Host
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )

    // Date picker dialogs
    MaterialDialog(
        dialogState = dateOfBirthDialogState,
        buttons = {
            positiveButton(text = "OK")
            negativeButton(text = "Cancel")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now().minusYears(30),
            title = "Select Date of Birth",
            yearRange = 1900..LocalDate.now().year,
            onDateChange = { selectedDate ->
                viewModel.dateOfBirth.value = viewModel.formatDate(selectedDate)
                dateOfBirthError.value = ValidationUtils.getDateError(viewModel.dateOfBirth.value, "Date of Birth")
            }
        )
    }

    MaterialDialog(
        dialogState = issueDateDialogState,
        buttons = {
            positiveButton(text = "OK")
            negativeButton(text = "Cancel")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now(),
            title = "Select Issue Date",
            yearRange = 2000..(LocalDate.now().year + 10),
            onDateChange = { selectedDate ->
                viewModel.issueDate.value = viewModel.formatDate(selectedDate)
                issueDateError.value = ValidationUtils.getDateError(viewModel.issueDate.value, "Issue Date")
                dateRangeError.value = ValidationUtils.getDateRangeError(viewModel.issueDate.value, viewModel.expireDate.value)
            }
        )
    }

    MaterialDialog(
        dialogState = expireDateDialogState,
        buttons = {
            positiveButton(text = "OK")
            negativeButton(text = "Cancel")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now().plusYears(10),
            title = "Select Expire Date",
            yearRange = LocalDate.now().year..(LocalDate.now().year + 20),
            onDateChange = { selectedDate ->
                viewModel.expireDate.value = viewModel.formatDate(selectedDate)
                expireDateError.value = ValidationUtils.getDateError(viewModel.expireDate.value, "Expire Date")
                dateRangeError.value = ValidationUtils.getDateRangeError(viewModel.issueDate.value, viewModel.expireDate.value)
            }
        )
    }
}

// Personal Information Section Composable with validation
@Composable
fun PersonalInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    age: String,
    onAgeChange: (String) -> Unit,
    ageError: String?,
    cnic: String,
    onCnicChange: (String) -> Unit,
    cnicError: String?,
    dateOfBirth: String,
    onDateOfBirthClick: () -> Unit,
    dateOfBirthError: String?,
    gender: String,
    onGenderChange: (String) -> Unit,
    genderError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Name *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ValidatedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = "Fatima Ali",
                    error = nameError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Age *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ValidatedTextField(
                    value = age,
                    onValueChange = onAgeChange,
                    placeholder = "30",
                    error = ageError,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column {
            Text(
                text = "CNIC *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = cnic,
                onValueChange = onCnicChange,
                placeholder = "XXXXX-XXXXXXX-X",
                error = cnicError,
                visualTransformation = CnicTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Date of Birth *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ValidatedDatePickerField(
                    value = dateOfBirth,
                    onClick = onDateOfBirthClick,
                    placeholder = "14-Sep-1999",
                    error = dateOfBirthError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gender *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ValidatedTextField(
                    value = gender,
                    onValueChange = onGenderChange,
                    placeholder = "Female",
                    error = genderError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Contact Information Section Composable with validation
@Composable
fun ContactInfoSection(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    phoneError: String?,
    temporaryAddress: String,
    onTemporaryAddressChange: (String) -> Unit,
    tempAddressError: String?,
    permanentAddress: String,
    onPermanentAddressChange: (String) -> Unit,
    permAddressError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Phone Number *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                placeholder = "03XX-XXXXXXX",
                error = phoneError,
                visualTransformation = PhoneTransformation(),
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            Text(
                text = "Temporary Address *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = temporaryAddress,
                onValueChange = onTemporaryAddressChange,
                placeholder = "House No. 12, Near Masjid-",
                error = tempAddressError,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            Text(
                text = "Permanent Address *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = permanentAddress,
                onValueChange = onPermanentAddressChange,
                placeholder = "House No. 12, Near Masjid, Matiari",
                error = permAddressError,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Location Information Section Composable with validation
@Composable
fun LocationInfoSection(
    district: String,
    onDistrictChange: (String) -> Unit,
    districtError: String?,
    taluka: String,
    onTalukaChange: (String) -> Unit,
    talukaError: String?,
    unionCouncil: String,
    onUnionCouncilChange: (String) -> Unit,
    unionCouncilError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "District *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = district,
                onValueChange = onDistrictChange,
                placeholder = "Matiari",
                error = districtError,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            Text(
                text = "Taluka *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = taluka,
                onValueChange = onTalukaChange,
                placeholder = "Hala",
                error = talukaError,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            Text(
                text = "Union Council-UC *",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ValidatedTextField(
                value = unionCouncil,
                onValueChange = onUnionCouncilChange,
                placeholder = "Union Council Pano Aqil",
                error = unionCouncilError,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Document Information Section Composable with validation
@Composable
fun DocumentInfoSection(
    issueDate: String,
    onIssueDateClick: () -> Unit,
    issueDateError: String?,
    expireDate: String,
    onExpireDateClick: () -> Unit,
    expireDateError: String?,
    dateRangeError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Issue Date *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ValidatedDatePickerField(
                    value = issueDate,
                    onClick = onIssueDateClick,
                    placeholder = "04-Sep-2024",
                    error = issueDateError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Expire Date *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ValidatedDatePickerField(
                    value = expireDate,
                    onClick = onExpireDateClick,
                    placeholder = "04-Sep-2034",
                    error = expireDateError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Date range error (appears below both date fields)
        dateRangeError?.let { error ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Beneficiary Status Section Composable with validation
@Composable
fun BeneficiaryStatusSection(
    beneficiaryStatus: BeneficiaryStatus?,
    beneficiaryStatusError: String?,
    onPregnantClick: () -> Unit,
    onLactatingClick: () -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onPregnantClick)
            ) {
                RadioButton(
                    selected = beneficiaryStatus == BeneficiaryStatus.PREGNANT,
                    onClick = onPregnantClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = primaryGreen,
                        unselectedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pregnant",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onLactatingClick)
            ) {
                RadioButton(
                    selected = beneficiaryStatus == BeneficiaryStatus.LACTATING,
                    onClick = onLactatingClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = primaryGreen,
                        unselectedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lactating",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }
        }
        // Error message for beneficiary status
        beneficiaryStatusError?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            modifier = modifier,
            maxLines = maxLines,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error == null) primaryGreen else Color.Red,
                unfocusedBorderColor = if (error == null) Color.Gray else Color.Red,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                errorCursorColor = Color.Red,
                errorBorderColor = Color.Red
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            ),
            singleLine = maxLines == 1,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType
            ),
            visualTransformation = visualTransformation,
            isError = error != null,
            supportingText = {
                error?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        )
    }
}

// Validated DatePicker Field Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidatedDatePickerField(
    value: String,
    onClick: () -> Unit,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error == null) primaryGreen else Color.Red,
                unfocusedBorderColor = if (error == null) Color.Gray else Color.Red,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                errorCursorColor = Color.Red,
                errorBorderColor = Color.Red
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            ),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Select date",
                        tint = if (error == null) primaryGreen else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            isError = error != null,
            supportingText = {
                error?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        )
    }
}

// CNIC Transformation (XXXXX-XXXXXXX-X)
class CnicTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 13) text.text.substring(0..12) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 4 || i == 11) {
                out += "-"
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 4 -> offset
                    offset <= 12 -> offset + 1
                    offset <= 13 -> offset + 2
                    else -> 15
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 5 -> offset
                    offset <= 13 -> offset - 1
                    else -> offset - 2
                }
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

// Phone Transformation (03XX-XXXXXXX)
class PhoneTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 14) text.text.substring(0..14) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 3) {
                out += "-"
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset <= 3) offset else offset + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= 4) offset else offset - 1
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}