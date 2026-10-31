package kekmech.ru.ui_map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_map_shared.APPEARANCE_HYBRID
import kekmech.ru.feature_map_shared.APPEARANCE_SCHEME
import kekmech.ru.feature_map_shared.MapController
import kekmech.ru.feature_map_shared.MapMarker
import kekmech.ru.feature_map_shared.MapTabs
import kekmech.ru.feature_map_shared.MarkerType

private const val LIST_HEIGHT_DP = 220

/** The map of the campus: the search and the tabs on top, the map in the middle, the list of the places below. */
@Composable
public fun MapScreen(controller: MapController, tileLoader: TileLoader) {
    val state by controller.state.collectAsState()
    val mapState = remember { MapState() }
    LaunchedEffect(controller) { controller.start() }
    val appearance = MapAppearance.fromCode(state.appearance)
    val visible = state.visible

    Column(Modifier.fillMaxSize().safeContentPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = controller::setQuery,
                label = { Text("Поиск мест") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                controller.setAppearance(
                    if (appearance == MapAppearance.SCHEME) APPEARANCE_HYBRID else APPEARANCE_SCHEME,
                )
            }) { Text(if (appearance == MapAppearance.SCHEME) "Спутник" else "Схема") }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MapTabs) { tab ->
                FilterChip(
                    selected = state.query.isBlank() && state.tab == tab,
                    onClick = { controller.selectTab(tab) },
                    label = { Text(tab.title()) },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            MpeiMap(
                state = mapState,
                markers = visible,
                selected = state.selected,
                appearance = appearance,
                tileLoader = tileLoader,
                onMarkerClick = controller::select,
                modifier = Modifier.fillMaxSize(),
            )
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    Modifier.align(Alignment.Center).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = controller::reload) { Text("Повторить") }
                }
                state.offline -> Text(
                    "Нет сети, места из сохранённой копии",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface).padding(4.dp),
                )
            }
        }
        PlacesList(
            places = visible,
            selected = state.selected,
            onClick = { place ->
                controller.select(place)
                mapState.camera = mapState.camera.lookingAt(place.location)
            },
        )
    }
}

@Composable
private fun PlacesList(places: List<MapMarker>, selected: MapMarker?, onClick: (MapMarker) -> Unit) {
    if (places.isEmpty()) {
        Text("Ничего не найдено", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(Modifier.fillMaxWidth().height(LIST_HEIGHT_DP.dp)) {
        items(places) { place ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (place.uid == selected?.uid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { onClick(place) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(place.name, style = MaterialTheme.typography.titleSmall)
                if (place.address.isNotBlank()) {
                    Text(place.address, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
        }
    }
}

private fun MarkerType.title(): String = when (this) {
    MarkerType.FOOD -> "Еда"
    MarkerType.BUILDING -> "Корпуса"
    MarkerType.HOSTEL -> "Общежития"
    MarkerType.STRUCTURE -> "Кафедры"
    MarkerType.OTHER, MarkerType.UNDEFINED -> "Прочее"
}
