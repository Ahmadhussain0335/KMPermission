import XCTest

/// Drives the sample through real system permission alerts. Meant for a physical device:
///
///     xcodebuild test -project sample-ios/iosApp.xcodeproj -scheme iosApp \
///       -destination 'platform=iOS,id=<device UDID>' TEAM_ID=<your team> -allowProvisioningUpdates
///
/// Notifications, Motion and Speech Recognition cannot be reset by XCUITest, so their tests only
/// prompt on a fresh install; afterwards they check that the stored answer is reported.
final class PermissionFlowUITests: XCTestCase {

    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    override func setUp() {
        continueAfterFailure = false
    }

    // MARK: - Scenarios

    func test01_cameraDenied_isReportedAsBlocked() {
        app.resetAuthorizationStatus(for: .camera)
        app.launch()

        request("Camera")
        answer(["Don’t Allow", "Don't Allow"])

        expectCompleted("Camera", "Blocked — use Settings")
    }

    func test02_microphoneAllowed_isGranted() {
        app.resetAuthorizationStatus(for: .microphone)
        app.launch()

        request("Microphone")
        answer(["Allow", "OK"])

        expectCompleted("Microphone", "Granted")
    }

    func test03_locationWithPreciseOff_isLimited() {
        app.resetAuthorizationStatus(for: .location)
        app.launch()

        request("Location")
        let alert = waitForAlert()
        let precise = alert.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Precise'")).firstMatch
        XCTAssertTrue(precise.exists, "No Precise toggle in: \(alertDescription(alert))")
        precise.tap()
        answer(["Allow While Using App", "Allow Once"])

        expectCompleted("Location", "Limited")
    }

    /// "Keep Only While Using" leaves the status unchanged and fires no delegate callback; the
    /// request must still finish, and later requests must not be blocked by it.
    func test04_backgroundUpgradeDeclined_doesNotHang() {
        app.resetAuthorizationStatus(for: .location)
        app.resetAuthorizationStatus(for: .contacts)
        app.launch()

        request("Location")
        answer(["Allow While Using App"])
        expectCompleted("Location", "Granted")

        request("Background Location")
        answer(["Keep Only While Using", "Keep While Using"])
        expectCompleted("Background Location", "Denied")

        request("Contacts")
        XCTAssertTrue(waitForAlert().exists, "A request after the declined upgrade never showed its prompt")
        answer(["Allow Full Access", "Continue", "Allow", "OK"])
        answerIfPresent(["Allow Full Access", "Allow"])
        // iOS 18+ follows with a "How do you want to share contacts?" sheet instead of an alert.
        tapIfPresent(NSPredicate(format: "label BEGINSWITH 'Share All'"))
        expectCompleted("Contacts", "Granted")
    }

    /// When In Use immediately followed by the Always upgrade: the second prompt appears while the
    /// first is still closing.
    func test05_backgroundFromNotDetermined_grantsAlways() {
        app.resetAuthorizationStatus(for: .location)
        app.launch()

        request("Background Location")
        answer(["Allow While Using App"])
        answer(["Change to Always Allow"])

        expectCompleted("Background Location", "Granted")
    }

    func test06_bluetoothAllowed_coversScanConnectAndAdvertise() {
        app.resetAuthorizationStatus(for: .bluetooth)
        app.launch()

        request("Bluetooth")
        answer(["Allow", "OK"])
        expectCompleted("Bluetooth", "Granted")

        request("Bluetooth Advertise")
        expectCompleted("Bluetooth Advertise", "Granted")
    }

    func test07_calendarFullAccess_isGranted() {
        app.resetAuthorizationStatus(for: .calendar)
        app.launch()

        request("Calendar")
        answer(["Allow Full Access", "Allow", "OK"])

        expectCompleted("Calendar", "Granted")
    }

    func test08_photosFullAccess_isGranted() {
        app.resetAuthorizationStatus(for: .photos)
        app.launch()

        request("Photos")
        answer(["Allow Full Access", "Allow Access to All Photos", "Allow"])
        answerIfPresent(["Allow Full Access", "Allow"])

        expectCompleted("Photos", "Granted")
    }

    func test09_notificationsMotionSpeech_resolve() {
        app.launch()

        for title in ["Notifications", "Motion", "Speech Recognition"] {
            request(title)
            answerIfPresent(["Allow", "OK"], timeout: 5)
            expectCompleted(title, "Granted")
        }
    }

    /// The camera was denied in test01. Turning it on in Settings must show up when the app returns.
    func test10_settingsChange_isPickedUpOnReturn() throws {
        app.launch()
        expectStatus("Camera", "Blocked — use Settings")

        tap(app.buttons["open-settings"])
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 10), "Open App Settings did not open Settings")

        let cameraSwitch = settings.switches["Camera"]
        guard cameraSwitch.waitForExistence(timeout: 10) else {
            throw XCTSkip("Camera switch not found on this iOS version's Settings page")
        }
        // A plain tap on an iOS 26 Settings switch can miss its control, so fall back to the inner
        // switch and then to the control's trailing edge.
        let attempts: [() -> Void] = [
            { cameraSwitch.tap() },
            { cameraSwitch.switches.firstMatch.tap() },
            { cameraSwitch.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)).tap() },
        ]
        for attempt in attempts where (cameraSwitch.value as? String) != "1" {
            attempt()
            Thread.sleep(forTimeInterval: 1)
        }
        let switchedOn = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == '1'"), object: cameraSwitch)
        XCTAssertEqual(XCTWaiter.wait(for: [switchedOn], timeout: 5), .completed, "Camera switch did not turn on")

        app.activate()
        expectStatus("Camera", "Granted")
    }

    // MARK: - Helpers

    private func request(_ title: String) {
        tap(app.buttons["request-\(title)"])
    }

    private func tap(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        scrollTo(element)
        XCTAssertTrue(element.exists && element.isHittable, "\(element) not found", file: file, line: line)
        element.tap()
    }

    private func visible(_ element: XCUIElement) -> Bool { element.exists && element.isHittable }

    /// List rows are created lazily, so swipe until the element exists and is on screen.
    private func scrollTo(_ element: XCUIElement) {
        _ = app.staticTexts["last-completed"].waitForExistence(timeout: 10)
        for _ in 0..<12 where !visible(element) { app.swipeUp(velocity: .slow) }
        for _ in 0..<12 where !visible(element) { app.swipeDown(velocity: .slow) }
    }

    /// Asserts that the request for [title] returned, via the marker only its completion sets.
    private func expectCompleted(_ title: String, _ expected: String, timeout: TimeInterval = 15, file: StaticString = #filePath, line: UInt = #line) {
        let marker = app.staticTexts["last-completed"]
        scrollTo(marker)
        let text = "Last completed: \(title): \(expected)"
        let result = XCTWaiter.wait(
            for: [XCTNSPredicateExpectation(predicate: NSPredicate(format: "label == %@", text), object: marker)],
            timeout: timeout
        )
        XCTAssertEqual(result, .completed, "Expected '\(text)', got '\(marker.exists ? marker.label : "<missing>")'", file: file, line: line)
    }

    @discardableResult
    private func waitForAlert(timeout: TimeInterval = 10) -> XCUIElement {
        let alert = springboard.alerts.firstMatch
        _ = alert.waitForExistence(timeout: timeout)
        return alert
    }

    private func answer(_ labels: [String], file: StaticString = #filePath, line: UInt = #line) {
        let alert = waitForAlert()
        XCTAssertTrue(alert.exists, "Expected a permission alert offering \(labels)", file: file, line: line)
        guard let button = labels.lazy.map({ alert.buttons[$0] }).first(where: { $0.exists }) else {
            XCTFail("None of \(labels) in \(alertDescription(alert))", file: file, line: line)
            return
        }
        button.tap()
    }

    private func answerIfPresent(_ labels: [String], timeout: TimeInterval = 3) {
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: timeout) else { return }
        labels.lazy.map { alert.buttons[$0] }.first(where: { $0.exists })?.tap()
    }

    /// Taps the first button matching [predicate] in the app or in a system process, if one appears.
    private func tapIfPresent(_ predicate: NSPredicate, timeout: TimeInterval = 10) {
        let candidates = [app.buttons.matching(predicate).firstMatch, springboard.buttons.matching(predicate).firstMatch]
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let button = candidates.first(where: { $0.exists && $0.isHittable }) {
                button.tap()
                return
            }
            Thread.sleep(forTimeInterval: 0.5)
        }
    }

    private func alertDescription(_ alert: XCUIElement) -> String {
        let buttons = alert.buttons.allElementsBoundByIndex.map(\.label)
        return "alert '\(alert.label)' with buttons \(buttons)"
    }

    private func expectStatus(_ title: String, _ expected: String, timeout: TimeInterval = 15, file: StaticString = #filePath, line: UInt = #line) {
        let status = app.staticTexts["status-\(title)"]
        scrollTo(status)
        let matches = NSPredicate(format: "label == %@", expected)
        let result = XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: matches, object: status)], timeout: timeout)
        XCTAssertEqual(result, .completed, "\(title): expected '\(expected)', got '\(status.exists ? status.label : "<missing>")'", file: file, line: line)
    }
}
