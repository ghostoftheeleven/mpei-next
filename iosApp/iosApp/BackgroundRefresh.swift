import BackgroundTasks
import Shared

/// The refresh of the app in the background: iOS wakes the app up from time to time (it decides when: usually a few times a
/// day, more often for the apps that are opened often), and the app looks for the changes in the schedule and plans the
/// reminders again. The work itself is in Kotlin (`runBackgroundRefresh` of the module `:shared`).
enum BackgroundRefresh {
    /// Must be the same as the one in `BGTaskSchedulerPermittedIdentifiers` of Info.plist (see project.yml).
    static let identifier = "ru.mpei.hub.refresh"

    /// Not earlier than in this time after the previous one; iOS can make it later.
    private static let minimumInterval: TimeInterval = 6 * 60 * 60

    /// Must be called before the app finishes launching (App.init).
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(refreshTask)
        }
    }

    /// Asks iOS for the next refresh; it is called when the app goes to the background and after every refresh.
    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: minimumInterval)
        // it fails in the simulator and when the background refresh is off in the settings: nothing to do about it
        try? BGTaskScheduler.shared.submit(request)
    }

    private static func handle(_ task: BGAppRefreshTask) {
        schedule()
        let cancel = IosBackgroundKt.runBackgroundRefresh { success in
            task.setTaskCompleted(success: success.boolValue)
        }
        task.expirationHandler = {
            _ = cancel()
        }
    }
}
