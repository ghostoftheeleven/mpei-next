package kekmech.ru.feature_app_update_impl

import kekmech.ru.feature_app_update_api.ForceUpdateChecker
import kekmech.ru.feature_app_update_api.ForceUpdateChecker.Companion.KEY_CURRENT_VERSION
import kekmech.ru.feature_app_update_api.ForceUpdateChecker.Companion.KEY_MIN_REQUIRED_VERSION
import kekmech.ru.feature_app_update_api.ForceUpdateChecker.Companion.KEY_UPDATE_DESCRIPTION
import kekmech.ru.feature_app_update_api.ForceUpdateChecker.Companion.KEY_UPDATE_REQUIRED
import kekmech.ru.feature_app_update_api.ForceUpdateChecker.Companion.KEY_UPDATE_URL
import kekmech.ru.feature_app_update_impl.domain.models.AppVersion
import kekmech.ru.feature_app_update_impl.domain.models.ForceUpdateInfo
import kekmech.ru.feature_app_update_impl.presentation.BlockingUpdateFragment
import kekmech.ru.feature_app_update_impl.presentation.ForceUpdateFragment
import kekmech.ru.lib_app_info.AppVersionName
import kekmech.ru.lib_feature_toggles.RemoteConfigWrapper
import kekmech.ru.lib_navigation.NewRoot
import kekmech.ru.lib_navigation.Router
import kekmech.ru.lib_navigation.ShowDialog

internal class ForceUpdateCheckerImpl(
    private val router: Router,
    private val appVersionName: AppVersionName,
    private val remoteConfig: RemoteConfigWrapper,
) : ForceUpdateChecker {

    override fun check() {
        val isUpdateRequired = remoteConfig.getUntyped(KEY_UPDATE_REQUIRED).toBoolean()
        val actualVersion = AppVersion(remoteConfig.getUntyped(KEY_CURRENT_VERSION))
        val appVersion = AppVersion(appVersionName.versionName)
        val updateUrl = remoteConfig.getUntyped(KEY_UPDATE_URL)
        val description = remoteConfig.getUntyped(KEY_UPDATE_DESCRIPTION)
        val minRequiredVersion = AppVersion(remoteConfig.getUntyped(KEY_MIN_REQUIRED_VERSION))

        val forceUpdateInfo = ForceUpdateInfo(
            actualVersion = actualVersion,
            updateUrl = updateUrl,
            shortDescription = description,
            isUpdateRequired = (actualVersion > appVersion) and isUpdateRequired
        )
        if (appVersion < minRequiredVersion) {
            router.executeCommand(NewRoot { BlockingUpdateFragment.newInstance(forceUpdateInfo) })
        } else if (forceUpdateInfo.isUpdateRequired) {
            router.executeCommand(ShowDialog {
                ForceUpdateFragment.newInstance(forceUpdateInfo)
            })
        }
    }
}
