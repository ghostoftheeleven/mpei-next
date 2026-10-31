package kekmech.ru.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import kekmech.ru.ui_icons.MpeixIcons
import kekmech.ru.ui_kit_navigationbar.NavigationBar
import kekmech.ru.ui_kit_navigationbar.NavigationBarItem

internal const val TAB_HOME = 0
internal const val TAB_SCHEDULE = 1
internal const val TAB_NOTES = 2
internal const val TAB_MAP = 3
internal const val TAB_BARS = 4

/** The bar with the tabs: the same component and the same icons as in the Android app. */
@Suppress("DEPRECATION")
@Composable
internal fun AppNavigationBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == TAB_HOME,
            onClick = { onSelect(TAB_HOME) },
            icon = { Icon(MpeixIcons.WhatshotBlack24, MpeixIcons.WhatshotOutline24) },
            label = "Главная",
        )
        NavigationBarItem(
            selected = selectedTab == TAB_SCHEDULE,
            onClick = { onSelect(TAB_SCHEDULE) },
            icon = { Icon(MpeixIcons.EventBlack24, MpeixIcons.EventOutline24) },
            label = "Расписание",
        )
        NavigationBarItem(
            selected = selectedTab == TAB_NOTES,
            onClick = { onSelect(TAB_NOTES) },
            icon = { Icon(Icons.Filled.Edit, Icons.Outlined.Edit) },
            label = "Заметки",
        )
        NavigationBarItem(
            selected = selectedTab == TAB_MAP,
            onClick = { onSelect(TAB_MAP) },
            icon = { Icon(MpeixIcons.ExploreBlack24, MpeixIcons.ExploreOutline24) },
            label = "Карта",
        )
        NavigationBarItem(
            selected = selectedTab == TAB_BARS,
            onClick = { onSelect(TAB_BARS) },
            icon = { Icon(MpeixIcons.AccountBlack24, MpeixIcons.AccountOutline24) },
            label = "БАРС",
        )
    }
}
