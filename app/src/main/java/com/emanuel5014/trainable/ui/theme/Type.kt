@file:OptIn(ExperimentalTextApi::class)
package com.emanuel5014.trainable.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.R

private val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex, weight = FontWeight.Thin, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 100f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.ExtraLight, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 200f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.Light, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 300f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.Normal, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 400f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.Medium, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 500f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.SemiBold, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 600f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.Bold, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 700f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.ExtraBold, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 800f), FontVariation.Setting("GRAD", 80f))),
    Font(R.font.roboto_flex, weight = FontWeight.Black, style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 900f), FontVariation.Setting("GRAD", 80f))),
)

private val SystemTypography = Typography()

val AppTypography = SystemTypography.run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.ExtraBold),
        displayMedium = displayMedium.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.ExtraBold),
        displaySmall = displaySmall.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.ExtraBold),
        headlineLarge = headlineLarge.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Medium),
        bodyMedium = bodyMedium.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Medium),
        bodySmall = bodySmall.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Medium),
        labelLarge = labelLarge.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = RobotoFlex, fontWeight = FontWeight.Medium),
    )
}
