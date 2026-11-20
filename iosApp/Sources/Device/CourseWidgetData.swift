import Foundation
import UIKit

struct DeviceCourse: Codable, Equatable {
    let day: Date
    let start: Date?
    let end: Date?
    let title: String
    let room: String
    let sections: String

    static var calendar: Calendar {
        var value = Calendar(identifier: .gregorian)
        value.timeZone = TimeZone(secondsFromGMT: 8 * 3600)!
        return value
    }

    static func clock(_ date: Date) -> String {
        let parts = calendar.dateComponents([.hour, .minute], from: date)
        return String(format: "%02d:%02d", parts.hour!, parts.minute!)
    }
}

struct CourseWidgetData: Codable, Equatable {
    let semester: String
    let courses: [DeviceCourse]
    var expires: Date?

    func today(at date: Date) -> [DeviceCourse] {
        courses.filter { DeviceCourse.calendar.isDate($0.day, inSameDayAs: date) }
    }

    func next(at date: Date) -> DeviceCourse? {
        courses.first { ($0.start ?? .distantPast) > date }
    }

    func temporarySnapshot(at date: Date) -> CourseWidgetData {
        let today = DeviceCourse.calendar.startOfDay(for: date)
        let following = DeviceCourse.calendar.date(byAdding: .day, value: 2, to: today)!
        var rows = courses.filter { $0.day >= today && $0.day < following }
        if let next = courses.first(where: { $0.day >= following && $0.start != nil }) { rows.append(next) }
        return CourseWidgetData(semester: semester, courses: rows, expires: following)
    }
}

enum WidgetSource {
    case linked(CourseWidgetData?), manual(CourseWidgetData), needsTransfer
}

@MainActor
enum CourseWidgetExchange {
    static let kind = "XatuOneCourseWidget"
    private static let group = "group.com.xatuone.campus"
    private static let boardName = UIPasteboard.Name("com.xatuone.campus.widget-transfer.v1")
    private static let payloadType = "com.xatuone.campus.widget-agenda"
    private static var file: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group)?
            .appendingPathComponent("course-widget.json")
    }
    static var hasContainer: Bool { file != nil }

    static func read() -> WidgetSource {
        if let file {
            let data = try? Data(contentsOf: file)
            return .linked(data.flatMap { try? JSONDecoder().decode(CourseWidgetData.self, from: $0) })
        }
        if let board = UIPasteboard(name: boardName, create: false),
           let bytes = board.data(forPasteboardType: payloadType),
           let value = try? JSONDecoder().decode(CourseWidgetData.self, from: bytes),
           let expiry = value.expires, expiry > Date() {
            return .manual(value)
        }
        return .needsTransfer
    }

    static func publish(_ data: CourseWidgetData?, allowTemporary: Bool) throws {
        if let file {
            if let data {
                try JSONEncoder().encode(data).write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            } else if FileManager.default.fileExists(atPath: file.path) {
                try FileManager.default.removeItem(at: file)
            }
        } else if allowTemporary, let data {
            guard let board = UIPasteboard(name: boardName, create: true) else {
                throw CocoaError(.fileWriteUnknown)
            }
            let snapshot = data.temporarySnapshot(at: Date())
            let bytes = try JSONEncoder().encode(snapshot)
            board.setItems([[payloadType: bytes]], options: [.localOnly: true, .expirationDate: snapshot.expires!])
        } else if UIPasteboard(name: boardName, create: false) != nil {
            UIPasteboard.remove(withName: boardName)
        }
    }
}
