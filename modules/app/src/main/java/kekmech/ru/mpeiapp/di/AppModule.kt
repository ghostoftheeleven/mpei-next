package kekmech.ru.mpeiapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kekmech.ru.lib_app_info.AppVersionName
import kekmech.ru.lib_feature_toggles.RemoteConfigWrapper
import kekmech.ru.mpeiapp.BuildConfig
import kekmech.ru.mpeiapp.RepoRemoteConfig
import kekmech.ru.mpeiapp.deeplink.di.DeeplinkModule
import kekmech.ru.mpeiapp.ui.main.di.MainScreenModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.Locale

val AppModule = module {
    single { Locale.GERMAN } bind Locale::class
    // the settings that change without a new version of the app: the file of the repository (statics/app/config.json)
    single { RepoRemoteConfig(androidContext(), get(), RepoRemoteConfig.defaults(BuildConfig.VERSION_NAME)) }
    single<RemoteConfigWrapper> { get<RepoRemoteConfig>() }
    factory { AppVersionName(BuildConfig.VERSION_NAME) }

    // TODO: figure out how to inject Dispatchers correctly
    @Suppress( "InjectDispatcher")
    single<DataStore<Preferences>> {
        val applicationContext: Context = androidApplication()
        val name = "mpeix.datastore.preferences"
        /**
         * Same implementation as in [androidx.datastore.preferences.preferencesDataStore] delegate
         */
        PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) {
            applicationContext.preferencesDataStoreFile(name)
        }
    }

    @Suppress("SpreadOperator")
    includes(
        MainScreenModule,
        DeeplinkModule,
        *FeatureModules,
        *LibraryModules,
    )
}
