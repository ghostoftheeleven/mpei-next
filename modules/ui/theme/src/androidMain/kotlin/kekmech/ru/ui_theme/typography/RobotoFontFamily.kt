package kekmech.ru.ui_theme.typography

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kekmech.ru.res_fonts.R

/**
 * Roboto as a plain value, for the Android screens that build a `TextStyle` outside of a composition. New code takes
 * [rememberRobotoFontFamily], which also works on iOS.
 */
public val RobotoFontFamily: FontFamily = FontFamily(
    Font(
        resId = R.font.roboto_regular,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.Async,
    ),
    Font(
        resId = R.font.roboto_medium,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.Async,
    ),
    Font(
        resId = R.font.roboto_bold,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.Async,
    ),
)
