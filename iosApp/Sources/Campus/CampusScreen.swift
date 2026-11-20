import CoreLocation
import SwiftUI
import UIKit

private struct CampusWebPage: Identifiable {
    let title: String
    let icon: String
    let url: URL
    var id: URL { url }

    init(_ title: String, _ icon: String, _ address: String) {
        self.title = title
        self.icon = icon
        self.url = URL(string: address)!
    }

    static let checkIn = Self("签到网页", "location", "https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/*default/index.do#/jrqd")
    static let daily = [
        Self("图书馆", "books.vertical", "https://lib.xatu.edu.cn/"),
        Self("校园网", "wifi", "https://self.xatu.edu.cn/"),
        Self("电费充值", "creditcard", "https://jfpay.xatu.edu.cn/casLogin/")
    ]
    static let services = [
        Self("请销假", "doc.text", "https://ehall.xatu.edu.cn/xsfw/sys/swmxsqjapp/*default/index.do?wxType=1"),
        Self("一站式大厅", "building.2", "https://ehall.xatu.edu.cn/"),
        Self("一网通办", "rectangle.grid.2x2", "https://ywtb.xatu.edu.cn/")
    ]
}

struct CampusScreen: View {
    @EnvironmentObject private var session: CampusSession
    @State private var search = ""
    private func matches(_ title: String) -> Bool {
        search.isEmpty || title.localizedCaseInsensitiveContains(search.trimmingCharacters(in: .whitespacesAndNewlines))
    }
    var body: some View {
        List {
            let daily = CampusWebPage.daily.filter { matches($0.title) }
            let services = CampusWebPage.services.filter { matches($0.title) }
            if matches("宿舍电费") || !daily.isEmpty {
                Section("校园生活") {
                    if matches("宿舍电费") { NavigationLink(destination: ElectricityScreen()) { Label("宿舍电费", systemImage: "bolt") } }
                    ForEach(daily) { webRow($0) }
                }
            }
            if matches("校园签到") || !services.isEmpty {
                Section("校园办事") {
                    if matches("校园签到") {
                        NavigationLink(destination: CampusCheckIn().id(session.accountID)) { Label("校园签到", systemImage: "location.circle") }
                    }
                    ForEach(services) { webRow($0) }
                }
            }
            if !matches("宿舍电费"), !matches("校园签到"), daily.isEmpty, services.isEmpty {
                Text("没有匹配的校园服务").foregroundStyle(.secondary)
            }
        }
        .navigationTitle("校园服务")
        .searchable(text: $search, prompt: "搜索校园服务")
    }

    private func webRow(_ page: CampusWebPage) -> some View {
        Button { session.open(page.url, title: page.title) } label: {
            HStack {
                Label(page.title, systemImage: page.icon)
                Spacer()
                Image(systemName: "globe").foregroundStyle(.secondary).accessibilityHidden(true)
            }
        }
        .accessibilityLabel(page.title + "，网页服务")
    }
}

private final class CampusLocation: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager: CLLocationManager
    @Published private(set) var permission: CLAuthorizationStatus
    @Published private(set) var position: CLLocation?
    @Published private(set) var locating = false
    @Published private(set) var error: String?
    private var awaitingPermission = false

    override init() {
        let manager = CLLocationManager()
        self.manager = manager
        permission = manager.authorizationStatus
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    func locate() {
        error = nil
        switch manager.authorizationStatus {
        case .notDetermined:
            awaitingPermission = true
            manager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            locating = true
            manager.requestLocation()
        case .denied, .restricted: break
        @unknown default: error = "无法获取定位授权状态"
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        permission = manager.authorizationStatus
        if permission == .denied || permission == .restricted { position = nil; locating = false }
        if awaitingPermission, permission != .notDetermined {
            awaitingPermission = false
            locate()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locating = false
        position = locations.last { $0.horizontalAccuracy >= 0 }
        error = position == nil ? "未能取得当前位置" : nil
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locating = false
        self.error = error.localizedDescription
    }
}

private struct CampusCheckIn: View {
    @EnvironmentObject private var session: CampusSession
    @StateObject private var location = CampusLocation()
    @Environment(\.openURL) private var openURL
    @State private var saved: CheckInPlace?
    @State private var saveError: String?

    var body: some View {
        List {
            Section {
                Button("打开校园签到网页") {
                    let page = CampusWebPage.checkIn
                    session.open(page.url, title: "校园签到")
                }
            }
            Section("设备位置") {
                LabeledContent("定位权限", value: permissionLabel)
                if location.permission == .denied {
                    Button("前往系统设置") { openURL(URL(string: UIApplication.openSettingsURLString)!) }
                } else if location.permission != .restricted {
                    if location.locating { ProgressView("正在定位") }
                    else { Button("获取当前位置") { location.locate() } }
                }
                if let position = location.position {
                    LabeledContent("纬度", value: position.coordinate.latitude.formatted(.number.precision(.fractionLength(5))))
                    LabeledContent("经度", value: position.coordinate.longitude.formatted(.number.precision(.fractionLength(5))))
                    LabeledContent("定位时间", value: position.timestamp.formatted(date: .abbreviated, time: .standard))
                    if !session.accountID.isEmpty {
                        Button("保存设备位置") {
                            do {
                                let place = CheckInPlace(position: position)
                                try place.save(account: session.accountID)
                                saved = place
                                saveError = nil
                            } catch { saveError = error.localizedDescription }
                        }
                    }
                }
                if let error = location.error { Text(error).foregroundStyle(.red) }
            }
            if let saved {
                Section("已保存的位置") {
                    LabeledContent("纬度", value: saved.latitude.formatted(.number.precision(.fractionLength(5))))
                    LabeledContent("经度", value: saved.longitude.formatted(.number.precision(.fractionLength(5))))
                    LabeledContent("定位时间", value: saved.time.formatted(date: .abbreviated, time: .standard))
                    Button("移除已保存位置", role: .destructive) {
                        CheckInPlace.clear(account: session.accountID)
                        self.saved = nil
                    }
                }
            }
            if let saveError { Text(saveError).foregroundStyle(.red) }
        }
        .navigationTitle("校园签到")
        .task(id: session.accountID) {
            do { saved = try CheckInPlace.load(account: session.accountID); saveError = nil }
            catch { saveError = error.localizedDescription }
        }
    }

    private var permissionLabel: String {
        switch location.permission {
        case .notDetermined: return "尚未授权"
        case .denied: return "未允许"
        case .restricted: return "系统限制"
        case .authorizedAlways: return "始终允许"
        case .authorizedWhenInUse: return "使用期间允许"
        @unknown default: return "未知"
        }
    }
}

struct CheckInPlace: Codable {
    let latitude: Double
    let longitude: Double
    let time: Date
    init(position: CLLocation) {
        latitude = position.coordinate.latitude
        longitude = position.coordinate.longitude
        time = position.timestamp
    }
    static func load(account: String, defaults: UserDefaults = .standard) throws -> Self? {
        guard !account.isEmpty, let bytes = defaults.data(forKey: "xatu.checkin.place.v1." + account) else { return nil }
        return try JSONDecoder().decode(Self.self, from: bytes)
    }
    func save(account: String, defaults: UserDefaults = .standard) throws {
        guard !account.isEmpty else { throw CampusSessionError.academicLoginRequired }
        defaults.set(try JSONEncoder().encode(self), forKey: "xatu.checkin.place.v1." + account)
    }
    static func clear(account: String, defaults: UserDefaults = .standard) {
        guard !account.isEmpty else { return }
        defaults.removeObject(forKey: "xatu.checkin.place.v1." + account)
    }
}
