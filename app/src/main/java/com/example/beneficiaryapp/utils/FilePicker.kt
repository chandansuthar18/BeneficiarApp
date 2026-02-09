// utils/FilePicker.kt
package com.example.beneficiaryapp.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberFilePicker(): Pair<List<Uri>, () -> Unit> {
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            selectedUris = uris
        }
    )

    val pickFiles = {
        launcher.launch("image/*")
    }

    return Pair(selectedUris, pickFiles)
}