package kekmech.ru.feature_map_impl.data

import kekmech.ru.feature_app_settings_api.data.AppSettingsRepository
import kekmech.ru.feature_map_shared.AppearanceStorage

/** The look of the map is a setting of the app: the settings screen changes it too, so both read and write the same place. */
internal class SettingsAppearanceStorage(
    private val appSettingsRepository: AppSettingsRepository,
) : AppearanceStorage {

    override fun get(): String = appSettingsRepository.getAppSettings().mapAppearanceType

    override fun set(appearance: String) {
        appSettingsRepository.updateAppSettings { copy(mapAppearanceType = appearance) }
    }
}
