package kekmech.ru.lib_analytics_android

import kekmech.ru.lib_analytics_android.dto.AnalyticsEvent

/**
 * Where the events of the screens (taps, scrolls, settings) go. They go nowhere: the app does not send the usage of the
 * app to anybody. The screens still describe their events through [Analytics], so a place to count them (a local log, a
 * server of the project) can be put here without touching the screens.
 */
internal class AnalyticsWrapper {

    @Suppress("UNUSED_PARAMETER")
    fun sendEvent(event: AnalyticsEvent) = Unit
}
