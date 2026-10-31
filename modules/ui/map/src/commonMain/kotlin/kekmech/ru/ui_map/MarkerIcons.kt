package kekmech.ru.ui_map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathParser
import kekmech.ru.feature_map_shared.MarkerType
import kekmech.ru.ui_map.resources.Res
import kekmech.ru.ui_map.resources.ic_emoji_block
import kekmech.ru.ui_map.resources.ic_emoji_force
import kekmech.ru.ui_map.resources.ic_emoji_hostels
import kekmech.ru.ui_map.resources.ic_emoji_library
import kekmech.ru.ui_map.resources.ic_emoji_masks
import kekmech.ru.ui_map.resources.ic_emoji_massage
import kekmech.ru.ui_map.resources.ic_emoji_pistol
import kekmech.ru.ui_map.resources.ic_emoji_sachok
import kekmech.ru.ui_map.resources.ic_emoji_sputnik
import kekmech.ru.ui_map.resources.ic_emoji_stadium
import kekmech.ru.ui_map.resources.ic_emoji_swimming_pool
import kekmech.ru.ui_map.resources.ic_hotel_24px
import kekmech.ru.ui_map.resources.ic_restaurant_24px
import kekmech.ru.ui_map.resources.ic_school_24px
import kekmech.ru.ui_map.resources.ic_star_rate_24px
import org.jetbrains.compose.resources.painterResource

/** The pin of the map: the same shape as `ic_place_32px` of the Android app (a 32 by 32 picture). */
internal const val PIN_VIEWPORT: Float = 32f
private const val PIN_PATH =
    "M16,2.6667C21.6,2.6667 26.6667,6.96 26.6667,13.6C26.6667,17.84 23.4,22.8267 16.88,28.5733" +
        "C16.3733,29.0133 15.6133,29.0133 15.1067,28.5733C8.6,22.8267 5.3333,17.84 5.3333,13.6" +
        "C5.3333,6.96 10.4,2.6667 16,2.6667Z"

internal val PinPath: Path by lazy { PathParser().parsePathString(PIN_PATH).toPath() }

/** The colors of the pins: the same as the colors of the Android app (colorPink, colorMain, colorGreen, colorPurple, colorTeal). */
internal fun MarkerType.pinColor(): Color = when (this) {
    MarkerType.FOOD -> Color(0xFFFF505C)
    MarkerType.BUILDING -> Color(0xFF587FC7)
    MarkerType.HOSTEL -> Color(0xFF578B0F)
    MarkerType.OTHER -> Color(0xFF673AB7)
    MarkerType.STRUCTURE, MarkerType.UNDEFINED -> Color(0xFF009688)
}

/**
 * The pictures of the markers. A marker whose `icon` is a name like `:library:` has its own picture instead of the pin;
 * the others are a pin with a letter (the buildings) or with the sign of their tab.
 */
@Stable
internal class MarkerIcons(
    private val named: Map<String, Painter>,
    private val decor: Map<MarkerType, Painter>,
) {

    /** The own picture of the marker, or `null`. */
    fun named(icon: String?): Painter? = icon?.let(named::get)

    /** The small sign in the pin of a tab, or `null` (the buildings have a letter). */
    fun decor(type: MarkerType): Painter? = decor[type]
}

@Composable
internal fun rememberMarkerIcons(): MarkerIcons {
    val named = mapOf(
        ":swimming_pool:" to painterResource(Res.drawable.ic_emoji_swimming_pool),
        ":library:" to painterResource(Res.drawable.ic_emoji_library),
        ":masks:" to painterResource(Res.drawable.ic_emoji_masks),
        ":block:" to painterResource(Res.drawable.ic_emoji_block),
        ":pistol:" to painterResource(Res.drawable.ic_emoji_pistol),
        ":sachok:" to painterResource(Res.drawable.ic_emoji_sachok),
        ":sputnik:" to painterResource(Res.drawable.ic_emoji_sputnik),
        ":force:" to painterResource(Res.drawable.ic_emoji_force),
        ":stadium:" to painterResource(Res.drawable.ic_emoji_stadium),
        ":hostels:" to painterResource(Res.drawable.ic_emoji_hostels),
        ":massage:" to painterResource(Res.drawable.ic_emoji_massage),
    )
    val decor = mapOf(
        MarkerType.FOOD to painterResource(Res.drawable.ic_restaurant_24px),
        MarkerType.HOSTEL to painterResource(Res.drawable.ic_hotel_24px),
        MarkerType.OTHER to painterResource(Res.drawable.ic_star_rate_24px),
        MarkerType.STRUCTURE to painterResource(Res.drawable.ic_school_24px),
    )
    return remember(named.values.toList(), decor.values.toList()) { MarkerIcons(named, decor) }
}
