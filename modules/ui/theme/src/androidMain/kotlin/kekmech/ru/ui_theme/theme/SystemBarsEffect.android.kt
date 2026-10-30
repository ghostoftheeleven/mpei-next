package kekmech.ru.ui_theme.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
internal actual fun SystemBarsEffect(darkTheme: Boolean) {
    if (LocalInspectionMode.current) return
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = !darkTheme,
        )
    }
}
