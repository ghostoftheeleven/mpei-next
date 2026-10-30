package kekmech.ru.ui_theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kekmech.ru.ui_theme.color.DarkMpeixPalette
import kekmech.ru.ui_theme.color.LightMpeixPalette
import kekmech.ru.ui_theme.color.MpeixColors
import kekmech.ru.ui_theme.color.MpeixPalette
import kekmech.ru.ui_theme.theme.MpeixTheme
import kekmech.ru.ui_theme.typography.RobotoFontFamily

@Composable
@Preview(device = "spec:width=1920px,height=1080px,dpi=240")
@Suppress("LongMethod")
private fun PalettePreview(
    @PreviewParameter(MpeixPaletteParameterProvider::class) palette: MpeixPalette,
) {
    val colorItem: @Composable (
        name: String,
        backgroundColor: Color,
        textColor: Color,
    ) -> Unit = { name, backgroundColor, textColor ->
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 48.dp)
                .background(backgroundColor)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text = name,
                color = textColor,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
    Column(
        modifier = Modifier
            .wrapContentSize()
            .background(
                color = palette.surface,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Color Palette",
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            color = palette.content,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(Modifier.padding(bottom = 12.dp)) {
            colorItem("Background", palette.background, palette.content)
            colorItem("Surface", palette.surface, palette.content)
            colorItem("Surface + 1", palette.surfacePlus1, palette.content)
            colorItem("Surface + 2", palette.surfacePlus2, palette.content)
            colorItem("Surface + 3", palette.surfacePlus3, palette.content)
            colorItem("Shimmer", palette.surfacePlus3, palette.content)
        }
        Row(Modifier.padding(bottom = 12.dp)) {
            colorItem("Primary", palette.primary, palette.contentAccent)
            colorItem("Secondary", palette.secondary, palette.contentAccent)
            colorItem("Tertiary", palette.tertiary, palette.contentAccent)
            colorItem("ContentAccent", palette.contentAccent, MpeixColors.Neutral10)
        }
        Row(Modifier.padding(bottom = 12.dp)) {
            colorItem("Primary\nContainer", palette.primaryContainer, palette.content)
            colorItem("Secondary\nContainer", palette.secondaryContainer, palette.content)
            colorItem("Content", palette.content, palette.surface)
            colorItem("ContentVariant", palette.contentVariant, palette.surface)
            colorItem("Content\nDisabled", palette.contentDisabled, palette.contentAccent)
            colorItem("Outline", palette.outline, palette.content)
        }
        Row {
            colorItem(
                "ClassesType\nLecture",
                LightMpeixPalette.classesTypeLecture,
                LightMpeixPalette.contentAccent,
            )
            colorItem(
                "ClassesType\nPractice",
                LightMpeixPalette.classesTypePractice,
                LightMpeixPalette.content,
            )
            colorItem(
                "ClassesType\nLab",
                LightMpeixPalette.classesTypeLab,
                LightMpeixPalette.contentAccent,
            )
        }
    }
}

internal class MpeixPaletteParameterProvider : PreviewParameterProvider<MpeixPalette> {

    override val values: Sequence<MpeixPalette>
        get() = sequenceOf(LightMpeixPalette, DarkMpeixPalette)
}

@Composable
@Preview(device = "spec:width=1920px,height=1080px,dpi=240")
@Suppress("LongMethod", "MagicNumber", "StringLiteralDuplication")
private fun TypographyPreview() {

    fun Modifier.drawDashedBorder(): Modifier =
        drawBehind {
            drawRoundRect(
                color = LightMpeixPalette.outline,
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(10f, 10f),
                        phase = 0f,
                    )
                ),
            )
        }

    val linePaddingModifier = Modifier.padding(bottom = 8.dp)

    MpeixTheme {
        Column(
            modifier = Modifier
                .background(
                    color = LightMpeixPalette.surface,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .drawDashedBorder()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Header H1 32/38",
                        style = MpeixTheme.typography.header1,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Header H2 28/36",
                        style = MpeixTheme.typography.header2,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Header H3 22/28",
                        style = MpeixTheme.typography.header3,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Header H4 20/24",
                        style = MpeixTheme.typography.header4,
                        color = LightMpeixPalette.content,
                    )
                }
                Spacer(Modifier.width(64.dp))
                Column {
                    Text(
                        text = "Заголовки экранов",
                        style = MpeixTheme.typography.header1,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Заголовки экранов",
                        style = MpeixTheme.typography.header2,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Заголовки экранов",
                        style = MpeixTheme.typography.header3,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Заголовки внутри блоков контента",
                        style = MpeixTheme.typography.header4,
                        color = LightMpeixPalette.content,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .drawDashedBorder()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Paragraph Big 16/24",
                        style = MpeixTheme.typography.paragraphBig,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Paragraph Normal 14/20",
                        style = MpeixTheme.typography.paragraphNormal,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Paragraph Big Accent 16/24",
                        style = MpeixTheme.typography.paragraphBigAccent,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Paragraph Normal Accent 14/20",
                        style = MpeixTheme.typography.paragraphNormalAccent,
                        color = LightMpeixPalette.content,
                    )
                }
                Spacer(Modifier.width(64.dp))
                Column {
                    Text(
                        text = "Текстовый контент, несущий много смысла",
                        style = MpeixTheme.typography.paragraphBig,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Короткие надписи внутри UI, подзаголовки внутри контента",
                        style = MpeixTheme.typography.paragraphNormal,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Текстовый контент, несущий много смысла",
                        style = MpeixTheme.typography.paragraphBigAccent,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Короткие надписи внутри UI, подзаголовки внутри контента",
                        style = MpeixTheme.typography.paragraphNormalAccent,
                        color = LightMpeixPalette.content,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .drawDashedBorder()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Label Big 14/20",
                        style = MpeixTheme.typography.labelBig,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Label Normal 12/16",
                        style = MpeixTheme.typography.labelNormal,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Label Mini 10/16",
                        style = MpeixTheme.typography.labelMini,
                        color = LightMpeixPalette.content,
                    )
                }
                Spacer(Modifier.width(64.dp))
                Column {
                    Text(
                        text = "Кнопки, поля ввода, ячейки",
                        style = MpeixTheme.typography.labelBig,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Ячейки и короткие надписи в UI",
                        style = MpeixTheme.typography.labelNormal,
                        color = LightMpeixPalette.content,
                        modifier = linePaddingModifier,
                    )
                    Text(
                        text = "Ячейки и короткие надписи в UI",
                        style = MpeixTheme.typography.labelMini,
                        color = LightMpeixPalette.content,
                    )
                }
            }
        }
    }

}
