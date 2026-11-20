import CryptoKit
import Foundation

struct AcademicSnapshot: Codable {
    let report: AcademicReport
    let updatedAt: Date
}

struct AcademicUpcomingExam {
    let semesterID: String
    let title: String
    let start: Date
    let room: String
    let updatedAt: Date
}

struct AcademicCache: Codable {
    var terms: [AcademicSemester] = []
    var termSelection: [String: String] = [:]
    var reports: [String: [String: AcademicSnapshot]] = [:]

    private static func file(account: String, directory: URL?) throws -> URL {
        let root = try directory ?? FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let accountKey = SHA256.hash(data: Data(account.utf8)).map { String(format: "%02x", $0) }.joined()
        return root.appendingPathComponent("xatu-academic-\(accountKey).json")
    }

    static func load(account: String, directory: URL? = nil) throws -> Self {
        guard !account.isEmpty else { return Self() }
        let url = try file(account: account, directory: directory)
        guard FileManager.default.fileExists(atPath: url.path) else { return Self() }
        return try JSONDecoder().decode(Self.self, from: Data(contentsOf: url))
    }

    static func update(account: String, directory: URL? = nil, _ change: (inout Self) -> Void) throws {
        guard !account.isEmpty else { throw CampusSessionError.academicLoginRequired }
        var cache = try load(account: account, directory: directory)
        change(&cache)
        let url = try file(account: account, directory: directory)
        try JSONEncoder().encode(cache).write(to: url, options: [.atomic, .completeFileProtection])
        NotificationCenter.default.post(name: .academicCacheChanged, object: nil)
    }

    static func clear(account: String, directory: URL? = nil) throws {
        guard !account.isEmpty else { return }
        let url = try file(account: account, directory: directory)
        if FileManager.default.fileExists(atPath: url.path) { try FileManager.default.removeItem(at: url) }
        NotificationCenter.default.post(name: .academicCacheChanged, object: nil)
    }

    static func nextExam(account: String, now: Date = Date(), directory: URL? = nil) throws -> AcademicUpcomingExam? {
        let cache = try load(account: account, directory: directory)
        let entries = cache.reports["exams"] ?? [:]
        return entries.flatMap { term, snapshot in
            snapshot.report.sections.flatMap(\.rows).compactMap { row -> AcademicUpcomingExam? in
                guard let start = row.examStart, start >= now else { return nil }
                return AcademicUpcomingExam(semesterID: term, title: row.title, start: start, room: row.value("考场") ?? "", updatedAt: snapshot.updatedAt)
            }
        }.min { $0.start == $1.start ? $0.updatedAt > $1.updatedAt : $0.start < $1.start }
    }
}

extension Notification.Name {
    static let academicCacheChanged = Notification.Name("XatuAcademicCacheChanged")
}

extension AcademicItem {
    func value(_ label: String) -> String? { fields.first { $0.label == label }?.value }

    func matches(_ query: String) -> Bool {
        [title, value("课程代码"), value("课程序号"), value("学期")].compactMap { $0 }.contains { $0.localizedCaseInsensitiveContains(query) }
    }

    var examStart: Date? {
        guard let raw = value("考试时间"),
              let m = AcademicParser.captures(#"(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?\s*[^\d]*?(\d{1,2})[:：](\d{2})"#, in: raw).first else { return nil }
        let c = DateComponents(year: Int(m[1]), month: Int(m[2]), day: Int(m[3]), hour: Int(m[4]), minute: Int(m[5]))
        let cal = AcademicParser.schoolCalendar
        guard c.isValidDate(in: cal) else { return nil }
        return cal.date(from: c)
    }

    func examCountdown(now: Date) -> String? {
        guard let date = examStart else { return nil }
        let cal = AcademicParser.schoolCalendar
        guard let days = cal.dateComponents([.day], from: cal.startOfDay(for: now), to: cal.startOfDay(for: date)).day, days >= 0 else { return nil }
        return days == 0 ? "今天考试" : "距考试 \(days) 天"
    }
}
