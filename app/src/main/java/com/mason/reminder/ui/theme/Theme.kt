package com.mason.reminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 紧急度配色
val CalmColor = androidx.compose.ui.graphics.Color(0xFF2E8B57)   // SeaGreen
val NoticeColor = androidx.compose.ui.graphics.Color(0xFFC107) // Amber
val UrgentColor = androidx.compose.ui.graphics.Color(0xFF9800) // Orange
val CriticalColor = androidx.compose.ui.graphics.Color(0xFFF44336) // Red

private val DarkColorScheme = darkColorScheme(
    primary = CalmColor,
    secondary = NoticeColor,
    tertiary = UrgentColor,
)

private val LightColorScheme = lightColorScheme(
    primary = CalmColor,
    secondary = NoticeColor,
    tertiary = UrgentColor,
)

@Composable
fun ReminderAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}