package com.coffeetime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.coffeetime.R

@OptIn(ExperimentalTextApi::class)
private fun fraunces(w: Int) = Font(R.font.fraunces_variable, weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("SOFT", 50f)))
@OptIn(ExperimentalTextApi::class)
private fun figtree(w: Int) = Font(R.font.figtree_variable, weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val FrauncesFamily = FontFamily(fraunces(400), fraunces(600))
val FigtreeFamily = FontFamily(figtree(400), figtree(600), figtree(700))

val CoffeeTypography = Typography(

    displayLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight(600), fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight(600), fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight(600), fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight(600), fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
// displayMedium y displaySmall quedan con los valores por defecto de M3: Coffee Time no los usa.

/** Montos: siempre cifras tabulares. */
object CoffeeType {
    val amountLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(700), fontSize = 36.sp, lineHeight = 44.sp, fontFeatureSettings = "tnum")
    val amountMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 20.sp, lineHeight = 28.sp, fontFeatureSettings = "tnum")
    val amountSmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum")
}

fun Double.asMoney(): String = "$%.2f".format(this)   // mismo formato que la consola
