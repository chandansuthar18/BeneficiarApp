package com.example.beneficiaryapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.presentation.theme.TopBar
import com.example.beneficiaryapp.presentation.theme.primaryGreen
import com.example.beneficiaryapp.viewmodels.BeneficiaryListViewModel
import com.example.beneficiaryapp.viewmodels.BeneficiaryUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBeneficiariesScreen(
    navController: NavHostController,
    viewModel: BeneficiaryListViewModel = hiltViewModel()
) {
    val beneficiaries by viewModel.beneficiaries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedBeneficiaryId by remember { mutableStateOf<String?>(null) }

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
                        text = "Beneficiaries (${beneficiaries.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = primaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search by name, CNIC or phone...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryGreen,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { viewModel.setFilterStatus(null) },
                    label = { Text("All") },
                    modifier = Modifier
                )
                FilterChip(
                    selected = filterStatus == BeneficiaryStatus.PREGNANT,
                    onClick = {
                        viewModel.setFilterStatus(
                            if (filterStatus == BeneficiaryStatus.PREGNANT) null
                            else BeneficiaryStatus.PREGNANT
                        )
                    },
                    label = { Text("Pregnant") },
                    modifier = Modifier
                )
                FilterChip(
                    selected = filterStatus == BeneficiaryStatus.LACTATING,
                    onClick = {
                        viewModel.setFilterStatus(
                            if (filterStatus == BeneficiaryStatus.LACTATING) null
                            else BeneficiaryStatus.LACTATING
                        )
                    },
                    label = { Text("Lactating") },
                    modifier = Modifier
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryGreen)
            }
        } else if (beneficiaries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No beneficiaries",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No beneficiaries found",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    if (searchQuery.isNotBlank() || filterStatus != null) {
                        TextButton(
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.setFilterStatus(null)
                            }
                        ) {
                            Text("Clear filters")
                        }
                    }
                }
            }
        } else {
            // Beneficiaries List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(beneficiaries) { beneficiary ->
                    BeneficiaryCard(
                        beneficiary = beneficiary,
                        onEdit = {
                            // Navigate to edit screen with beneficiary ID
                            navController.navigate("edit_beneficiary/${beneficiary.id}")
                        },
                        onDelete = {
                            selectedBeneficiaryId = beneficiary.id
                            showDeleteDialog = true
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedBeneficiaryId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Beneficiary") },
            text = { Text("Are you sure you want to delete this beneficiary? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBeneficiary(selectedBeneficiaryId!!)
                        showDeleteDialog = false
                        selectedBeneficiaryId = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedBeneficiaryId = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BeneficiaryCard(
    beneficiary: BeneficiaryUI,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Profile Image
                if (beneficiary.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(beneficiary.imageUrls.first())
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Name and Basic Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = beneficiary.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${beneficiary.age} years • ${beneficiary.gender}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "CNIC: ${beneficiary.cnic}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Phone: ${beneficiary.phoneNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (beneficiary.status) {
                                BeneficiaryStatus.PREGNANT -> Color(0xFFE91E63).copy(alpha = 0.1f)
                                BeneficiaryStatus.LACTATING -> Color(0xFF2196F3).copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = beneficiary.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (beneficiary.status) {
                            BeneficiaryStatus.PREGNANT -> Color(0xFFE91E63)
                            BeneficiaryStatus.LACTATING -> Color(0xFF2196F3)
                        },
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Details
            Text(
                text = beneficiary.statusDetails,
                style = MaterialTheme.typography.bodyMedium,
                color = when (beneficiary.status) {
                    BeneficiaryStatus.PREGNANT -> Color(0xFFE91E63)
                    BeneficiaryStatus.LACTATING -> Color(0xFF2196F3)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Expanded Details (shown when card is clicked)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Address Information
                Column {
                    Text(
                        text = "Address Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Temporary: ${beneficiary.temporaryAddress}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Permanent: ${beneficiary.permanentAddress}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Location: ${beneficiary.district}, ${beneficiary.taluka}, ${beneficiary.unionCouncil}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Additional Information
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Date of Birth",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = beneficiary.dateOfBirth,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Document Issue",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = beneficiary.issueDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Document Expiry",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = beneficiary.expireDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                // Pregnancy/Lactating Specific Details
                when (beneficiary.status) {
                    BeneficiaryStatus.PREGNANT -> {
                        if (beneficiary.pregnancyWeek != null || beneficiary.gravida != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (beneficiary.pregnancyWeek != null) {
                                    Column {
                                        Text(
                                            text = "Pregnancy Week",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = beneficiary.pregnancyWeek,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (beneficiary.gravida != null) {
                                    Column {
                                        Text(
                                            text = "Gravida/Para",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "G${beneficiary.gravida}P${beneficiary.para}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    BeneficiaryStatus.LACTATING -> {
                        if (beneficiary.deliveryDate != null || beneficiary.childrenCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (beneficiary.deliveryDate != null) {
                                    Column {
                                        Text(
                                            text = "Delivery Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = beneficiary.deliveryDate!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Children",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${beneficiary.childrenCount} children",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Proof Images Gallery
                if (beneficiary.imageUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Proof Images (${beneficiary.imageUrls.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(beneficiary.imageUrls) { imageUrl ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Proof Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/Collapse Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Edit Button
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = primaryGreen
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Button
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}