import Foundation
import SwiftUI
import UserNotifications
import WidgetKit

@MainActor
final class CourseDeviceServices: ObservableObject {
    static let shared = CourseDeviceServices()

    @Published private(set) var enabled = UserDefaults.standard.bool(forKey: "courseReminders.enabled")
    @Published private(set) var minutes = UserDefaults.standard.object(forKey: "courseReminders.minutes") as? Int ?? 10
    @Published private(set) var notificationMessage = ""
    @Published private(set) var widgetMessage = ""
    @Published private(set) var askingPermission = false
    @Published private(set) var hasTimetable = false

    private let center = UNUserNotificationCenter.current()
    private let prefix = "com.xatuone.campus.course."
    private var owner: String?
    private var temporaryOwner: String?
    private var agenda: CourseWidgetData?
    private var revision = 0
    private var running = false
    private var clearDelivered = false

    func refresh(account: String) {
        let defaults = UserDefaults.standard
        if owner != account {
            if defaults.string(forKey: "courseReminders.owner") != account {
                clearDelivered = true
                defaults.set(account, forKey: "courseReminders.owner")
            }
            temporaryOwner = nil
            owner = account
        }
        do {
            let table = account.isEmpty ? nil : try TimetableStore().load(account: account).current
            agenda = table.map { CourseDevicePlan.agenda(for: $0, now: Date()) }
            hasTimetable = table != nil
            publishWidget()
        } catch {
            agenda = nil
            hasTimetable = false
            publishWidget()
            widgetMessage = "暂时无法读取当前课表，请解锁后重新打开。"
        }
        schedule()
    }

    func transferWidget() {
        guard hasTimetable, let owner else { return }
        temporaryOwner = owner
        publishWidget()
    }

    private func publishWidget() {
        do {
            try CourseWidgetExchange.publish(agenda, allowTemporary: owner != nil && temporaryOwner == owner)
            if CourseWidgetExchange.hasContainer {
                widgetMessage = hasTimetable ? "小组件会随当前课表更新。" : "添加或同步课表后，小组件会显示课程。"
            } else if temporaryOwner != nil && hasTimetable {
                widgetMessage = "已临时同步。若系统结束主应用，请重新打开并同步。"
            } else {
                widgetMessage = "此签名使用临时同步。在这里发送当前课表后，将小组件添加到主屏幕。"
            }
        } catch {
            widgetMessage = "小组件数据未能更新：\(error.localizedDescription)"
        }
        WidgetCenter.shared.reloadTimelines(ofKind: CourseWidgetExchange.kind)
    }

    func setEnabled(_ value: Bool) async {
        guard !askingPermission else { return }
        if value {
            askingPermission = true
            defer { askingPermission = false }
            do {
                guard try await center.requestAuthorization(options: [.alert, .sound]) else {
                    notificationMessage = "请在系统设置中允许一校通发送通知。"
                    return
                }
            } catch {
                notificationMessage = error.localizedDescription
                return
            }
        } else {
            clearDelivered = true
        }
        enabled = value
        UserDefaults.standard.set(value, forKey: "courseReminders.enabled")
        schedule()
    }

    func setMinutes(_ value: Int) {
        minutes = value
        UserDefaults.standard.set(value, forKey: "courseReminders.minutes")
        schedule()
    }

    // Serialize system updates so an already-running old account pass finishes before the new one cleans up.
    private func schedule() {
        revision += 1
        guard !running else { return }
        running = true
        Task {
            while true {
                let pass = revision
                let pending = await center.pendingNotificationRequests()
                center.removePendingNotificationRequests(withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix(prefix) })
                if clearDelivered {
                    clearDelivered = false
                    let items = await center.deliveredNotifications()
                    center.removeDeliveredNotifications(withIdentifiers: items.map(\.request.identifier).filter { $0.hasPrefix(prefix) })
                }
                if enabled, let agenda {
                    let status = await center.notificationSettings().authorizationStatus
                    if [.authorized, .provisional, .ephemeral].contains(status) {
                        var count = 0
                        do {
                            let requests = CourseDevicePlan.reminders(in: agenda, now: Date(), minutes: minutes)
                            for (index, request) in requests.enumerated() where pass == revision {
                                let (date, course) = request
                                let content = UNMutableNotificationContent()
                                content.title = course.title
                                content.body = [course.start.map(DeviceCourse.clock) ?? "", course.room]
                                    .filter { !$0.isEmpty }.joined(separator: " · ")
                                content.sound = .default
                                var parts = DeviceCourse.calendar.dateComponents([.year, .month, .day, .hour, .minute], from: date)
                                parts.calendar = DeviceCourse.calendar
                                parts.timeZone = DeviceCourse.calendar.timeZone
                                let trigger = UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
                                try await center.add(UNNotificationRequest(identifier: prefix + String(index),
                                                                           content: content, trigger: trigger))
                                count += 1
                            }
                            notificationMessage = count == 0 ? "当前没有待提醒的课程。" : "已安排最近 \(count) 次上课提醒。"
                        } catch {
                            notificationMessage = "已安排 \(count) 次提醒，其余未能保存：\(error.localizedDescription)"
                        }
                    } else {
                        notificationMessage = "请在系统设置中允许一校通发送通知。"
                    }
                } else {
                    notificationMessage = enabled ? "添加或同步课表后会安排提醒。" : "提醒已关闭。"
                }
                if pass == revision { break }
            }
            running = false
        }
    }
}

struct CourseReminderSettings: View {
    @ObservedObject private var services = CourseDeviceServices.shared
    @Environment(\.openURL) private var openURL

    var body: some View {
        Form {
            Section {
                Toggle("上课通知", isOn: Binding(get: { services.enabled }, set: { value in
                    Task { await services.setEnabled(value) }
                })).disabled(services.askingPermission)
                Picker("提前提醒", selection: Binding(get: { services.minutes }, set: services.setMinutes)) {
                    ForEach([0, 5, 10, 15, 30, 60], id: \.self) { minutes in
                        Text(minutes == 0 ? "上课时" : "\(minutes) 分钟").tag(minutes)
                    }
                }
                Button("打开系统通知设置") {
                    if let url = URL(string: UIApplication.openNotificationSettingsURLString) { openURL(url) }
                }
            } header: { Text("课程提醒") } footer: {
                Text(services.notificationMessage + " 重新打开应用会补充后续提醒。")
            }
            Section {
                if !CourseWidgetExchange.hasContainer {
                    Button("临时同步小组件") { services.transferWidget() }.disabled(!services.hasTimetable)
                }
                Text(services.widgetMessage).font(.footnote).foregroundStyle(.secondary)
            } header: { Text("主屏幕小组件") }
        }
        .navigationTitle("提醒与小组件")
    }
}
