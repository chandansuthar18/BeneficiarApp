package com.example.beneficiaryapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val errorRed = Color(0xFFD32F2F)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3498DB),
    secondary = Color(0xFF2C3E50),
    tertiary = Color(0xFF1ABC9C),
    background = Color(0xFFECF0F1),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2C3E50),
    onSurface = Color(0xFF2C3E50),

)

@Composable
fun BeneficiaryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}