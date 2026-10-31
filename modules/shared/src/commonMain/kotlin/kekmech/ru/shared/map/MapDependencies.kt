package kekmech.ru.shared.map

import kekmech.ru.feature_map_shared.MapMarkersRepository
import kekmech.ru.lib_settings.KeyValueStore
import kekmech.ru.ui_map.TileFetcher

/** What the map needs from the platform: the markers, the way to load a tile, and where the settings are kept. */
public class MapDependencies(
    public val markers: MapMarkersRepository,
    public val tiles: TileFetcher,
    public val settings: KeyValueStore,
)
