import Foundation

struct TimetableCourse: Codable, Equatable {
    var name: String
    var teacher: String
    var position: String
    var day: Int
    var startSection: Int
    var endSection: Int
    var weeks: [Int]
    var credit: Double?
    var isCustomTime: Bool?
    var customStartTime: String?
    var customEndTime: String?
    var color: Int?
    var remark: String?

    var title: String { name.components(separatedBy: " | ").first ?? name }
    var number: String? {
        guard let separator = name.range(of: " | ", options: .backwards) else { return nil }
        return String(name[separator.upperBound...])
    }
    var sectionText: String {
        startSection == endSection ? "第 \(startSection) 节" : "第 \(startSection)–\(endSection) 节"
    }
}

struct TeachingSlot: Codable, Equatable {
    var number: Int
    var startTime: String
    var endTime: String
    var alias: String?
}

struct WeeklyTeachingPeriod: Identifiable {
    let number: Int
    let alias: String?
    let commonSlot: TeachingSlot?
    var id: Int { number }
}

struct SchoolTermDates: Codable, Equatable {
    var semesterStartDate: String
    var semesterTotalWeeks: Int
    var firstDayOfWeek: Int?
    var defaultClassDuration: Int?
    var defaultBreakDuration: Int?
}

struct TimetableComboSchedule: Codable, Equatable {
    var name: String?
    var publicSchedules: [TimetableScheduleRule]
}

struct TimetableScheduleRule: Codable, Equatable {
    var name: String
    var startDate: String
    var endDate: String
    var defaultClassDuration: Int?
    var defaultBreakDuration: Int?
    var timeSlots: [TeachingSlot]
}

struct TimetableDayAdjustment: Codable, Equatable {
    var label: String
    // Actual calendar date is the dictionary key. nil cancels that day; a date supplies its regular classes.
    var lessonDate: String?
}

struct TimetableDisplay: Codable, Equatable {
    var showNumber = false
    var showTeacher = true
    var showLocation = true
}

struct TimetableDraft: Decodable {
    var semester: String
    var courses: [TimetableCourse]
    var slots: [TeachingSlot]
    var config: SchoolTermDates?

    var lastWeek: Int { courses.flatMap(\.weeks).max() ?? 1 }

    func usableDates(previous: CampusTimetable?) -> SchoolTermDates? {
        let candidate = config ?? (previous?.semester == semester ? previous?.dates : nil)
        guard let dates = candidate, SchoolCalendar.date(dates.semesterStartDate) != nil,
              dates.semesterTotalWeeks >= lastWeek else { return nil }
        return dates
    }
}

enum ScheduleIssue: LocalizedError {
    case message(String)
    var errorDescription: String? { if case let .message(text) = self { return text }; return nil }
}

enum SchoolCalendar {
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 8 * 3600)!
        calendar.firstWeekday = 2
        return calendar
    }
    static func date(_ text: String) -> Date? {
        let parts = text.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let result = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2])),
              iso(result) == text else { return nil }
        return result
    }
    static func iso(_ date: Date) -> String {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year!, parts.month!, parts.day!)
    }
    static func dayNumber(_ date: Date) -> Int { (calendar.component(.weekday, from: date) + 5) % 7 + 1 }
    static func add(_ days: Int, to date: Date) -> Date { calendar.date(byAdding: .day, value: days, to: date)! }
    static func monday(of date: Date) -> Date { add(1 - dayNumber(date), to: calendar.startOfDay(for: date)) }
    static let dayNames = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
}

struct CampusTimetable: Codable, Equatable {
    var semester: String
    var dates: SchoolTermDates
    var courses: [TimetableCourse]
    var slots: [TeachingSlot]
    var displayName: String?
    var display: TimetableDisplay?
    var adjustments: [String: TimetableDayAdjustment]?
    var comboSchedule: TimetableComboSchedule?

    var title: String { displayName ?? semester }
    var displayOptions: TimetableDisplay { display ?? TimetableDisplay() }
    func startOfWeek(containing date: Date) -> Date {
        let first = dates.firstDayOfWeek ?? 1
        let offset = (SchoolCalendar.dayNumber(date) - first + 7) % 7
        return SchoolCalendar.add(-offset, to: SchoolCalendar.calendar.startOfDay(for: date))
    }

    func week(on date: Date) -> Int? {
        guard let start = SchoolCalendar.date(dates.semesterStartDate), date >= start else { return nil }
        let calendar = SchoolCalendar.calendar
        guard let elapsed = calendar.dateComponents([.day], from: startOfWeek(containing: start),
                                                   to: calendar.startOfDay(for: date)).day, elapsed >= 0 else { return nil }
        let week = elapsed / 7 + 1
        return week <= dates.semesterTotalWeeks ? week : nil
    }

    func pageWeek(containing date: Date) -> Int? {
        week(on: SchoolCalendar.add(6, to: startOfWeek(containing: date)))
    }

    func date(inWeek week: Int, keepingWeekdayOf date: Date) -> Date? {
        guard week >= 1, week <= dates.semesterTotalWeeks,
              let start = SchoolCalendar.date(dates.semesterStartDate) else { return nil }
        let offset = (SchoolCalendar.dayNumber(date) - (dates.firstDayOfWeek ?? 1) + 7) % 7
        return max(start, SchoolCalendar.add((week - 1) * 7 + offset, to: startOfWeek(containing: start)))
    }

    func coursesInWeek(containing date: Date) -> [[TimetableCourse]] {
        let first = startOfWeek(containing: date)
        return (0..<7).map { courses(on: SchoolCalendar.add($0, to: first)) }
    }

    func courses(on date: Date) -> [TimetableCourse] {
        guard week(on: date) != nil else { return [] }
        var lessonDate = date
        if let adjustment = adjustments?[SchoolCalendar.iso(date)] {
            guard let source = adjustment.lessonDate.flatMap(SchoolCalendar.date) else { return [] }
            lessonDate = source
        }
        guard let week = week(on: lessonDate) else { return [] }
        let weekday = SchoolCalendar.dayNumber(lessonDate)
        return courses.filter { $0.day == weekday && $0.weeks.contains(week) }
            .sorted { ($0.startSection, $0.name) < ($1.startSection, $1.name) }
    }

    func slots(on date: Date) -> [TeachingSlot] {
        let day = SchoolCalendar.iso(date)
        guard let rule = comboSchedule?.publicSchedules.first(where: { $0.startDate <= day && day <= $0.endDate })
        else { return slots }
        if slots.isEmpty { return rule.timeSlots }
        return slots.map { base in rule.timeSlots.first(where: { $0.number == base.number }) ?? base }
    }

    func weekPeriods(days: [Date], coursesByDay: [[TimetableCourse]]) -> [WeeklyTeachingPeriod] {
        let daily = days.map { slots(on: $0) }
        // An imported custom-time course may have no corresponding row in the saved period table.
        let numbers = Set(daily.flatMap { $0.map(\.number) } + coursesByDay.flatMap { $0.map(\.startSection) })
        return numbers.sorted().map { number in
            let slots = daily.compactMap { $0.first { $0.number == number } }
            let first = slots.first
            let uniform = slots.count == days.count && slots.allSatisfy {
                $0.startTime == first?.startTime && $0.endTime == first?.endTime
            }
            return WeeklyTeachingPeriod(number: number, alias: first?.alias, commonSlot: uniform ? first : nil)
        }
    }

    func time(for course: TimetableCourse, on date: Date? = nil) -> String? {
        if course.isCustomTime == true, let start = course.customStartTime, let end = course.customEndTime {
            return "\(start)–\(end)"
        }
        let effective = date.map { slots(on: $0) } ?? slots
        guard let start = effective.first(where: { $0.number == course.startSection })?.startTime,
              let end = effective.first(where: { $0.number == course.endSection })?.endTime else { return nil }
        return "\(start)–\(end)"
    }
}

struct AccountTimetables: Codable, Equatable {
    var selected: String?
    var semesters: [String: CampusTimetable] = [:]
    var current: CampusTimetable? { selected.flatMap { semesters[$0] } }
}

struct TimetableStore {
    let directory: URL
    init(directory: URL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("Timetables", isDirectory: true)) {
        self.directory = directory
    }
    private func file(for account: String) throws -> URL {
        guard !account.isEmpty, account.allSatisfy({ $0.isHexDigit }) else { throw ScheduleIssue.message("请先登录教务账号") }
        return directory.appendingPathComponent(account + ".json")
    }
    func load(account: String) throws -> AccountTimetables {
        let file = try file(for: account)
        guard FileManager.default.fileExists(atPath: file.path) else { return AccountTimetables() }
        return try JSONDecoder().decode(AccountTimetables.self, from: Data(contentsOf: file))
    }
    func save(_ value: AccountTimetables, account: String) throws {
        let destination = try file(for: account)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try JSONEncoder().encode(value).write(to: destination, options: [.atomic, .completeFileProtection])
        NotificationCenter.default.post(name: .xatuTimetableChanged, object: account)
    }
}
