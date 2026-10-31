package kekmech.ru.lib_analytics_android.di

import kekmech.ru.lib_analytics_android.AnalyticsWrapper
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

public val LibraryAnalyticsModule: Module = module {
    factoryOf(::AnalyticsWrapper)
}
