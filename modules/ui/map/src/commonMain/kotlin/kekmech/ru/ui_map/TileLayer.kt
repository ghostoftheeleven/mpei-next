package kekmech.ru.ui_map

import kekmech.ru.feature_map_shared.TileId

/** One layer of tiles: the images that [url] points to, up to the zoom level [maxLevel]. */
public class TileLayer(
    public val id: String,
    public val maxLevel: Int,
    public val url: (TileId) -> String,
)

/** What the map shows: the street scheme, or the satellite imagery with the names of the places on top. */
public enum class MapAppearance(public val code: String, public val layers: List<TileLayer>, public val attribution: String) {
    SCHEME(
        code = "scheme",
        layers = listOf(
            TileLayer(id = "osm", maxLevel = 19) { "https://tile.openstreetmap.org/${it.z}/${it.x}/${it.y}.png" },
        ),
        attribution = "© OpenStreetMap contributors",
    ),
    HYBRID(
        code = "hybrid",
        layers = listOf(
            TileLayer(id = "esri-imagery", maxLevel = 19) { esriTile("World_Imagery", it) },
            TileLayer(id = "esri-places", maxLevel = 19) {
                esriTile("Reference/World_Boundaries_and_Places", it)
            },
        ),
        attribution = "© Esri, Maxar, Earthstar Geographics",
    );

    public companion object {
        /** The appearance that is saved under [code] in the settings; the scheme if there is none. */
        public fun fromCode(code: String?): MapAppearance = entries.firstOrNull { it.code == code } ?: SCHEME
    }
}

private const val ESRI_BASE = "https://server.arcgisonline.com/ArcGIS/rest/services/"

/** The tiles of Esri are addressed as `.../tile/{z}/{y}/{x}`: the row goes before the column. */
private fun esriTile(service: String, id: TileId): String =
    "$ESRI_BASE$service/MapServer/tile/${id.z}/${id.y}/${id.x}"
