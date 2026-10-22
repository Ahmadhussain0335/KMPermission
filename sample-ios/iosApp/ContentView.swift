import SwiftUI
import Combine
import KMPPermissionCore

private struct PermissionDemoItem: Identifiable {
    let id = UUID()
    let title: String
    let permissions: [Permission]
}

private let demoItems: [PermissionDemoItem] = [
    PermissionDemoItem(title: "Camera", permissions: [Permission.Camera.shared]),
    PermissionDemoItem(title: "Microphone", permissions: [Permission.Microphone.shared]),
    PermissionDemoItem(
        title: "Bluetooth",
        permissions: [Permission.Bluetooth.shared, Permission.BluetoothScan.shared, Permission.BluetoothConnect.shared]
    ),
    PermissionDemoItem(title: "Location", permissions: [Permission.LocationFine.shared, Permission.LocationCoarse.shared]),
    PermissionDemoItem(title: "Background Location", permissions: [Permission.LocationBackground.shared]),
    PermissionDemoItem(title: "Notifications", permissions: [Permission.Notifications.shared]),
    PermissionDemoItem(title: "Contacts", permissions: [Permission.Contacts.shared]),
    PermissionDemoItem(title: "Calendar", permissions: [Permission.Calendar.shared]),
    PermissionDemoItem(title: "Photos", permissions: [Permission.Photos.shared]),
    PermissionDemoItem(title: "Motion", permissions: [Permission.Motion.shared]),
    PermissionDemoItem(title: "Speech Recognition", permissions: [Permission.SpeechRecognition.shared]),
]

private func worstStatus(_ statuses: [PermissionStatus]) -> PermissionStatus {
    if statuses.contains(where: { $0 is PermissionStatusPermanentlyDenied }) { return PermissionStatusPermanentlyDenied.shared }
    if statuses.contains(where: { $0 is PermissionStatusRestricted }) { return PermissionStatusRestricted.shared }
    if statuses.contains(where: { $0 is PermissionStatusDenied }) { return PermissionStatusDenied.shared }
    if statuses.contains(where: { $0 is PermissionStatusNotDetermined }) { return PermissionStatusNotDetermined.shared }
    if statuses.contains(where: { $0 is PermissionStatusLimited }) { return PermissionStatusLimited.shared }
    return PermissionStatusGranted.shared
}

private func label(for status: PermissionStatus) -> String {
    switch status {
    case is PermissionStatusGranted: return "Granted"
    case is PermissionStatusLimited: return "Limited"
    case is PermissionStatusDenied: return "Denied"
    case is PermissionStatusPermanentlyDenied: return "Blocked — use Settings"
    case is PermissionStatusRestricted: return "Restricted"
    case is PermissionStatusNotDetermined: return "Not requested yet"
    default: return "Unknown"
    }
}

private func color(for status: PermissionStatus) -> Color {
    switch status {
    case is PermissionStatusGranted: return .green
    case is PermissionStatusLimited: return .mint
    case is PermissionStatusDenied: return .orange
    case is PermissionStatusPermanentlyDenied: return .red
    case is PermissionStatusRestricted: return .gray
    default: return .secondary
    }
}

private final class PermissionDemoViewModel: ObservableObject {
    private let manager = PermissionManager_iosKt.PermissionManager()
    @Published private var statuses: [String: PermissionStatus] = [:]

    func status(for item: PermissionDemoItem) -> PermissionStatus {
        let known = item.permissions.map { statuses[key(for: $0)] ?? PermissionStatusNotDetermined.shared }
        return worstStatus(known)
    }

    func refreshInitialStatuses() {
        for item in demoItems {
            for permission in item.permissions {
                statuses[key(for: permission)] = manager.check(permission: permission)
            }
        }
    }

    func request(_ item: PermissionDemoItem) {
        // One batch call per row: the library resolves the permissions in order behind one prompt
        // queue instead of racing separate requests against each other.
        manager.request(permissions: item.permissions) { [weak self] results in
            guard let self else { return }
            DispatchQueue.main.async {
                for (permission, status) in results {
                    self.statuses[self.key(for: permission)] = status
                }
            }
        }
    }

    func openSettings() {
        manager.openSettings()
    }

    private func key(for permission: Permission) -> String {
        String(describing: permission)
    }
}

struct ContentView: View {
    @StateObject private var viewModel = PermissionDemoViewModel()

    var body: some View {
        NavigationView {
            List {
                ForEach(demoItems) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.title).font(.headline)
                            Text(label(for: viewModel.status(for: item)))
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(color(for: viewModel.status(for: item)))
                                .foregroundColor(.white)
                                .clipShape(Capsule())
                        }
                        Spacer()
                        Button("Request") {
                            viewModel.request(item)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.vertical, 4)
                }
                Section {
                    Button("Open App Settings") {
                        viewModel.openSettings()
                    }
                }
            }
            .navigationTitle("KMPermission Sample")
            .onAppear { viewModel.refreshInitialStatuses() }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
