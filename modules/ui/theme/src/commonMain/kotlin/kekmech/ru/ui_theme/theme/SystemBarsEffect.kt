package kekmech.ru.ui_theme.theme

import androidx.compose.runtime.Composable

/** Makes the system bars transparent with icons that are visible on the current theme (Android only). */
@Composable
internal expect fun SystemBarsEffect(darkTheme: Boolean)
