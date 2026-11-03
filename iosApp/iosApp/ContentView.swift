import SwiftUI
import Shared

/// The shared Compose UI of the Kotlin module, filling the screen.
struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosAppKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
