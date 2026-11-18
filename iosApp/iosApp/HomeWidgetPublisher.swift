import Foundation
import Shared
import WidgetKit

/// Kotlin builds the widgets' snapshot (IosHomeWidgets); this writes it into the App Group and redraws the widgets.
final class HomeWidgetPublisher: NSObject, IosHomeWidgetPublisher {
    func publish(json: String) {
        guard HomeWidgetStore.write(json) else { return }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
