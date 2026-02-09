package com.example.beneficiaryapp.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.ChildData
import com.example.beneficiaryapp.presentation.theme.TopBar
import com.example.beneficiaryapp.presentation.theme.primaryGreen
import com.example.beneficiaryapp.utils.NetworkMonitor
import com.example.beneficiaryapp.viewmodels.BeneficiaryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LactatingScreen(navController: NavHostController? = null) {
    val viewModel: BeneficiaryViewModel = hiltViewModel()
    val genderOptions = listOf("Male", "Female")
    val context = LocalContext.current

    // Initialize children data with empty children based on count
    var selectedChildren by remember { mutableStateOf(1) }
    val childrenData = remember { mutableStateListOf<ChildData>() }

    // ✅ FIXED: Load basic info when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadBasicInfoFromStorage()

        // Initialize children data
        if (childrenData.isEmpty()) {
            childrenData.addAll(List(selectedChildren) { ChildData(name = "", gender = "") })
            viewModel.childrenData.value = childrenData.toList()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Date picker dialog state
    val deliveryDateDialogState = rememberMaterialDialogState()

    // Date formatter for display
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

    // Storage permissions for Android 13+
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    // Network monitoring
    val networkMonitor = NetworkMonitor(context)
    val isOnline by networkMonitor.isOnline.collectAsState(initial = false)

    // Validate form
    val isFormValid = remember(viewModel.deliveryDate.value, childrenData, selectedChildren) {
        viewModel.deliveryDate.value.isNotBlank() &&
                childrenData
                    .take(selectedChildren)
                    .all { child ->
                        child.name.trim().isNotEmpty() &&
                                child.gender.trim().isNotEmpty()
                    }
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

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
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
                            text = "Lactating Details",
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
                        text = "Lactating Details",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 22.sp
                    )
                }

                // Delivery Date Field
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Delivery Date *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.deliveryDate.value,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = {
                            Text(
                                "14-Sep-1999",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { deliveryDateDialogState.show() }) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Calendar",
                                    tint = primaryGreen
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deliveryDateDialogState.show() },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )
                }

                // Number of Children Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Add Child Details:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3).forEach { count ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedChildren = count
                                        // Update children data list
                                        while (childrenData.size < count) {
                                            childrenData.add(ChildData(name = "", gender = ""))
                                        }
                                        while (childrenData.size > count) {
                                            childrenData.removeLast()
                                        }
                                        // Update viewModel
                                        viewModel.childrenData.value = childrenData.toList()
                                    }
                            ) {
                                // Number Circle
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedChildren == count) primaryGreen else Color.LightGray
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedChildren == count) primaryGreen else Color.Gray,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedChildren == count) Color.White else Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (count == 1) "Child" else "Children",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedChildren == count) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedChildren == count)
                                        primaryGreen
                                    else Color.Gray
                                )
                            }
                        }
                    }
                }

                // Horizontal Divider
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    color = Color.LightGray,
                    thickness = 1.dp
                )

                // Children Details Sections with Cards
                (1..selectedChildren).forEach { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        ChildDetailSection(
                            childNumber = index,
                            childData = childrenData.getOrNull(index - 1) ?: ChildData(),
                            onUpdate = { updatedData ->
                                if (index - 1 < childrenData.size) {
                                    childrenData[index - 1] = updatedData
                                } else {
                                    childrenData.add(updatedData)
                                }
                                // Update viewModel
                                viewModel.childrenData.value = childrenData.toList()
                            },
                            genderOptions = genderOptions,
                            context = context,
                            permissionState = permissionState,
                            snackbarHostState = snackbarHostState,
                            scope = scope
                        )
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
                            if (isFormValid) {
                                // Ensure beneficiary status is set to LACTATING
                                if (viewModel.beneficiaryStatus.value != BeneficiaryStatus.LACTATING) {
                                    viewModel.setBeneficiaryStatus(BeneficiaryStatus.LACTATING)
                                }

                                // Get all proof URIs from children
                                val allProofUris = childrenData
                                    .take(selectedChildren)
                                    .flatMap { it.proofUris }

                                // Set proof images in viewModel
                                viewModel.setProofImages(allProofUris)

                                // Save complete beneficiary data (offline-first)
                                viewModel.saveCompleteBeneficiaryData(
                                    isPregnancy = false
                                )
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Please fill all required fields",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
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
                                text = "Submit Lactating Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delivery Date Date Picker Dialog
    MaterialDialog(
        dialogState = deliveryDateDialogState,
        buttons = {
            positiveButton(text = "OK")
            negativeButton(text = "Cancel")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now().minusYears(2),
            title = "Select Delivery Date",
            yearRange = 1990..LocalDate.now().year,
            onDateChange = { selectedDate ->
                // Update viewModel's deliveryDate
                viewModel.deliveryDate.value = selectedDate.format(dateFormatter)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChildDetailSection(
    childNumber: Int,
    childData: ChildData,
    onUpdate: (ChildData) -> Unit,
    genderOptions: List<String>,
    context: Context,
    permissionState: com.google.accompanist.permissions.MultiplePermissionsState,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var genderExpanded by remember { mutableStateOf(false) }

    // File picker for multiple proof documents
    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val updatedProofs = childData.proofUris + uris.map { it.toString() }
                onUpdate(childData.copy(proofUris = updatedProofs))
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "${uris.size} proof document(s) uploaded",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Child Section Title
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (childNumber) {
                    1 -> "First Child Detail:"
                    2 -> "Second Child Detail:"
                    else -> "Third Child Detail:"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = primaryGreen
            )
        }

        // Child Name Field
        Column {
            Text(
                text = "Child Name *",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = childData.name,
                onValueChange = {
                    onUpdate(childData.copy(name = it))
                },
                placeholder = {
                    Text(
                        when (childNumber) {
                            1 -> "Abdullah"
                            2 -> "Fatima"
                            else -> "Enter child name"
                        },
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryGreen,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )
        }

        // Gender Field
        Column {
            Text(
                text = "Gender *",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                OutlinedTextField(
                    value = childData.gender,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = {
                        Text(
                            when (childNumber) {
                                1 -> "Male"
                                2 -> "Female"
                                else -> "Select gender"
                            },
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = genderExpanded
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
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option,
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                onUpdate(childData.copy(gender = option))
                                genderExpanded = false
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        // Proof of Childbirth Upload Section
        Column {
            Text(
                text = "Upload proof of childbirth:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Upload Button with Image Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        // Check permissions before opening file picker
                        if (permissionState.allPermissionsGranted) {
                            multipleImagePicker.launch("image/*")
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = "Upload Proof",
                        tint = Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Upload Text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (childData.proofUris.isNotEmpty())
                            "${childData.proofUris.size} document(s) uploaded"
                        else
                            "Upload proof image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to upload files",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Photos, PDFs, Documents (max 25MB each)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Uploaded Proofs Preview Section
        if (childData.proofUris.isNotEmpty()) {
            Column {
                Text(
                    text = "Uploaded Proofs:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Display uploaded proof thumbnails
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(childData.proofUris.size) { index ->
                        val uri = childData.proofUris[index]
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            // Show image thumbnail
                            Image(
                                painter = rememberAsyncImagePainter(model = uri),
                                contentDescription = "Proof ${index + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        // Optionally show full image on click
                                    },
                                contentScale = ContentScale.Crop
                            )

                            // Remove button
                            IconButton(
                                onClick = {
                                    val updatedProofs = childData.proofUris.toMutableList()
                                    updatedProofs.removeAt(index)
                                    onUpdate(childData.copy(proofUris = updatedProofs))
                                },
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.Red.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}