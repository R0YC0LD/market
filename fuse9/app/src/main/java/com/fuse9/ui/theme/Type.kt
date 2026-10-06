package com.fuse9.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.fuse9.R

/** Manrope for numerals and UI (calm, open digits); Fraunces, soft-cut, for the few words that matter. */
@OptIn(ExperimentalTextApi::class)
object FuseFonts {
    private fun manrope(weight: Int) = Font(
        R.font.manrope, FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    private fun fraunces(weight: Int) = Font(
        R.font.fraunces, FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("SOFT", 100f),
            FontVariation.Setting("opsz", 72f),
        ),
    )

    val Sans = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700))
    val Serif = FontFamily(fraunces(300), fraunces(400), fraunces(600))
}
