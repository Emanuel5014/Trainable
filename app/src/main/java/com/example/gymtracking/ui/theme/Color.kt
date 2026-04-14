package com.example.gymtracking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MonolithicSurface = Color(0xFF0E0E11)
val MonolithicSurfaceContainerLow = Color(0xFF141317)
val MonolithicSurfaceContainer = Color(0xFF1A191E)
val MonolithicSurfaceContainerHigh = Color(0xFF201F25)
val MonolithicSurfaceContainerHighest = Color(0xFF26252C)

val MonolithicPrimary = Color(0xFF42A5F5)
val MonolithicOnPrimary = Color(0xFF003258)

val MonolithicSecondaryContainer = Color(0xFF292336)
val MonolithicOnSecondaryContainer = Color(0xFFA9A0B9)

val MonolithicTertiary = Color(0xFFFFD9E3)
val MonolithicTertiaryContainer = Color(0xFFFEC5D6)
val MonolithicOnTertiary = Color(0xFF492534)
val MonolithicOnTertiaryContainer = Color(0xFF492534)

val MonolithicOnSurface = Color(0xFFE7E1EC)
val MonolithicOnSurfaceVariant = Color(0xFFADA9B3)
val MonolithicOutlineVariant = Color(0xFF49474F)

val MonolithicError = Color(0xFFFFB4AB)
val MonolithicOnError = Color(0xFF690005)

val Surface: Color
	@Composable get() = MaterialTheme.colorScheme.surface

val SurfaceContainerLow: Color
	@Composable get() = MaterialTheme.colorScheme.surfaceContainerLow

val SurfaceContainer: Color
	@Composable get() = MaterialTheme.colorScheme.surfaceContainer

val SurfaceContainerHigh: Color
	@Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

val SurfaceContainerHighest: Color
	@Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

val Primary: Color
	@Composable get() = MaterialTheme.colorScheme.primary

val OnPrimary: Color
	@Composable get() = MaterialTheme.colorScheme.onPrimary

val SecondaryContainer: Color
	@Composable get() = MaterialTheme.colorScheme.secondaryContainer

val OnSecondaryContainer: Color
	@Composable get() = MaterialTheme.colorScheme.onSecondaryContainer

val Tertiary: Color
	@Composable get() = MaterialTheme.colorScheme.tertiary

val TertiaryContainer: Color
	@Composable get() = MaterialTheme.colorScheme.tertiaryContainer

val OnTertiary: Color
	@Composable get() = MaterialTheme.colorScheme.onTertiary

val OnTertiaryContainer: Color
	@Composable get() = MaterialTheme.colorScheme.onTertiaryContainer

val OnSurface: Color
	@Composable get() = MaterialTheme.colorScheme.onSurface

val OnSurfaceVariant: Color
	@Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val OutlineVariant: Color
	@Composable get() = MaterialTheme.colorScheme.outlineVariant

val Error: Color
	@Composable get() = MaterialTheme.colorScheme.error

val OnError: Color
	@Composable get() = MaterialTheme.colorScheme.onError