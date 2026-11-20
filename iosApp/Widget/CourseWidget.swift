import SwiftUI
import WidgetKit

private struct CourseEntry: TimelineEntry {
    let date: Date
    let source: WidgetSource
}

private struct CourseProvider: TimelineProvider {
    func placeholder(in context: Context) -> CourseEntry {
        CourseEntry(date: Date(), source: .linked(nil))
    }

    func getSnapshot(in context: Context, completion: @escaping (CourseEntry) -> Void) {
        Task { @MainActor in completion(CourseEntry(date: Date(), source: CourseWidgetExchange.read())) }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CourseEntry>) -> Void) {
        Task { @MainActor in
            let now = Date()
            let source = CourseWidgetExchange.read()
            let calendar = DeviceCourse.calendar
            let tomorrow = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: now))!
            let data: CourseWidgetData?
            switch source {
            case .linked(let agenda): data = agenda
            case .manual(let agenda): data = agenda
            case .needsTransfer: data = nil
            }
            var changes = Set((data?.today(at: now) ?? []).compactMap(\.start).filter { $0 > now })
            changes.insert(now)
            // At midnight the new day is visible even when WidgetKit grants its next reload late.
            changes.insert(tomorrow)
            let entries = changes.sorted().map { CourseEntry(date: $0, source: source) }
            completion(Timeline(entries: entries, policy: .after(tomorrow)))
        }
    }
}

private struct CourseWidgetView: View {
    let entry: CourseEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Label("一校通 · 课程", systemImage: "calendar").font(.caption.bold()).foregroundStyle(.secondary)
            switch entry.source {
            case .linked(let data):
                if let data { agenda(data) }
                else { Text("打开一校通添加课表").font(.headline) }
            case .manual(let data): agenda(data)
            case .needsTransfer:
                Text("打开一校通").font(.headline)
                Text("在“提醒与小组件”中临时同步课表。").font(.caption)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .widgetURL(URL(string: "xatuone://schedule"))
        .modifier(WidgetBackground())
    }

    @ViewBuilder private func agenda(_ data: CourseWidgetData) -> some View {
        let today = data.today(at: entry.date)
        Text(today.isEmpty ? "今天没有课" : "今天 \(today.count) 节课").font(.headline)
        let limit = family == .systemLarge ? 6 : (family == .systemMedium ? 2 : 0)
        if limit > 0 {
            ForEach(Array(today.prefix(limit).enumerated()), id: \.offset) { _, course in
                HStack {
                    Text(course.start.map(DeviceCourse.clock) ?? course.sections).font(.caption.monospacedDigit())
                    Text(course.title).font(.subheadline).lineLimit(1)
                    Spacer(minLength: 0)
                    if !course.room.isEmpty { Text(course.room).font(.caption).lineLimit(1) }
                }.privacySensitive()
            }
        }
        if let next = data.next(at: entry.date), let start = next.start {
            let isToday = DeviceCourse.calendar.isDate(start, inSameDayAs: entry.date)
            let parts = DeviceCourse.calendar.dateComponents([.month, .day], from: start)
            let day = isToday ? "" : "\(parts.month!)/\(parts.day!) "
            Text("下一节 · \(day)\(DeviceCourse.clock(start))").font(.caption).foregroundStyle(.secondary)
            Text(next.title).font(.subheadline.bold()).lineLimit(2).privacySensitive()
        } else {
            Text("暂无后续定时课程").font(.caption).foregroundStyle(.secondary)
        }
    }
}

private struct WidgetBackground: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17, *) {
            content.containerBackground(.background, for: .widget)
        } else {
            content.padding(12).background(Color(.systemBackground))
        }
    }
}

@main
struct XatuCourseWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "XatuOneCourseWidget", provider: CourseProvider()) { entry in
            CourseWidgetView(entry: entry)
        }
        .configurationDisplayName("今日课表")
        .description("查看今天的课程和下一节课。")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
