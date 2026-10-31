package kekmech.ru.feature_map_impl.di

import io.ktor.client.HttpClient
import kekmech.ru.ext_shared_preferences.asKeyValueStore
import kekmech.ru.feature_map_api.MapFeatureLauncher
import kekmech.ru.feature_map_api.data.repository.MapRepository
import kekmech.ru.feature_map_impl.data.SettingsAppearanceStorage
import kekmech.ru.feature_map_impl.data.network.MapService
import kekmech.ru.feature_map_impl.data.repository.MapRepositoryImpl
import kekmech.ru.feature_map_impl.launcher.DeeplinkDelegate
import kekmech.ru.feature_map_impl.launcher.MapFeatureLauncherImpl
import kekmech.ru.feature_map_impl.presentation.MapViewModel
import kekmech.ru.feature_map_shared.AppearanceStorage
import kekmech.ru.feature_map_shared.MapMarkersRepository
import kekmech.ru.lib_app_info.AppVersionName
import kekmech.ru.lib_network.buildApi
import kekmech.ru.ui_map.tileHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

private const val MARKERS_PREFERENCES = "map_markers"
private const val BUNDLED_MARKERS = "markers.json"

val FeatureMapModule = module {
    singleOf(::DeeplinkDelegate) // TODO: refactor this
    factoryOf(::MapFeatureLauncherImpl) bind MapFeatureLauncher::class

    // the common map: the markers (with the copy on the device and the copy that comes with the app) and the look of the map
    single {
        MapMarkersRepository(
            client = get<HttpClient>(),
            store = androidContext().getSharedPreferences(MARKERS_PREFERENCES, android.content.Context.MODE_PRIVATE)
                .asKeyValueStore(),
            bundled = {
                androidContext().assets.open(BUNDLED_MARKERS).bufferedReader().use { it.readText() }
            },
        )
    }
    single<AppearanceStorage> { SettingsAppearanceStorage(get()) }
    factory {
        MapViewModel(
            repository = get(),
            appearance = get(),
            // the tiles come from third parties, so they are loaded by a client that does not tell the id of the device
            tileClient = tileHttpClient("${androidContext().packageName}/${get<AppVersionName>().versionName}"),
        )
    }

    // the markers for the search of the places
    single { get<Retrofit.Builder>().buildApi<MapService>() } bind MapService::class
    factoryOf(::MapRepositoryImpl) bind MapRepository::class
}
