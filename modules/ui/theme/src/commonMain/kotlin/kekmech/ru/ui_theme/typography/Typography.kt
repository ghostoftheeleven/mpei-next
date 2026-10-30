package kekmech.ru.ui_theme.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kekmech.ru.ui_theme.resources.Res
import kekmech.ru.ui_theme.resources.roboto_bold
import kekmech.ru.ui_theme.resources.roboto_medium
import kekmech.ru.ui_theme.resources.roboto_regular
import org.jetbrains.compose.resources.Font

/**
 * Roboto from the Compose resources of this module (`composeResources/font`). A font of Compose Multiplatform can
 * only be loaded inside a composition, that is why it is a function and not a value.
 */
@Composable
public fun rememberRobotoFontFamily(): FontFamily {
    val regular = Font(Res.font.roboto_regular, weight = FontWeight.Normal, style = FontStyle.Normal)
    val medium = Font(Res.font.roboto_medium, weight = FontWeight.Medium, style = FontStyle.Normal)
    val bold = Font(Res.font.roboto_bold, weight = FontWeight.Bold, style = FontStyle.Normal)
    return remember(regular, medium, bold) { FontFamily(regular, medium, bold) }
}

@Immutable
public class MpeixTypography internal constructor(fontFamily: FontFamily) {

    public val header1: TextStyle = style(fontFamily, size = 32, line = 38, FontWeight.Normal)
    public val header2: TextStyle = style(fontFamily, size = 28, line = 36, FontWeight.Normal)
    public val header3: TextStyle = style(fontFamily, size = 22, line = 28, FontWeight.Normal)
    public val header4: TextStyle = style(fontFamily, size = 20, line = 24, FontWeight.Medium)
    public val paragraphBig: TextStyle = style(fontFamily, size = 16, line = 24, FontWeight.Normal)
    public val paragraphNormal: TextStyle = style(fontFamily, size = 14, line = 20, FontWeight.Normal)
    public val paragraphBigAccent: TextStyle = style(fontFamily, size = 16, line = 24, FontWeight.Bold)
    public val paragraphNormalAccent: TextStyle = style(fontFamily, size = 14, line = 20, FontWeight.Bold)
    public val labelBig: TextStyle = style(fontFamily, size = 14, line = 20, FontWeight.Medium)
    public val labelNormal: TextStyle = style(fontFamily, size = 12, line = 16, FontWeight.Medium)
    public val labelMini: TextStyle = style(fontFamily, size = 10, line = 16, FontWeight.Medium)

    private companion object {

        fun style(fontFamily: FontFamily, size: Int, line: Int, weight: FontWeight) = TextStyle(
            fontFamily = fontFamily,
            fontSize = size.sp,
            lineHeight = line.sp,
            fontWeight = weight,
            fontStyle = FontStyle.Normal,
        )
    }
}
