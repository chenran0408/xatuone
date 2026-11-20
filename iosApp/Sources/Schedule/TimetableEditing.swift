import Foundation

extension TeachingSlot {
    static let campusDefaults: [TeachingSlot] = [
        ("08:20", "09:05"), ("09:15", "10:00"), ("10:20", "11:05"), ("11:15", "12:00"),
        ("14:00", "14:45"), ("14:55", "15:40"), ("16:00", "16:45"), ("16:55", "17:40"),
        ("18:10", "18:55"), ("19:05", "19:50"), ("20:00", "20:45"), ("20:55", "21:40")
    ].enumerated().map { TeachingSlot(number: $0.offset + 1, startTime: $0.element.0, endTime: $0.element.1) }

    static func minutes(_ text: String) -> Int? {
        let p = text.split(separator: ":")
        guard p.count == 2, p[0].count == 2, p[1].count == 2, let h = Int(p[0]), let m = Int(p[1]),
              (0..<24).contains(h), (0..<60).contains(m) else { return nil }
        return h * 60 + m
    }
    static func valid(_ slots: [TeachingSlot]) -> Bool {
        var previousEnd = 0
        for (index, slot) in slots.enumerated() {
            guard slot.number == index + 1, let start = minutes(slot.startTime), let end = minutes(slot.endTime),
                  start >= previousEnd, end > start else { return false }
            previousEnd = end
        }
        return !slots.isEmpty
    }
}

extension TimetableCourse {
    static func parseWeeks(_ text: String, limit: Int) throws -> [Int] {
        let parts = text.components(separatedBy: CharacterSet(charactersIn: ",，、"))
        var result = Set<Int>()
        for part in parts {
            let range = part.trimmingCharacters(in: .whitespaces).components(separatedBy: "-")
            guard (1...2).contains(range.count), let start = Int(range[0]), let end = Int(range.last ?? ""),
                  start >= 1, start <= end, end <= limit else {
                throw ScheduleIssue.message("周次请输入 1 至 \(limit) 之间的数字或范围，例如 1-4,6")
            }
            result.formUnion(start...end)
        }
        return result.sorted()
    }
}

extension CampusTimetable {
    mutating func adjustCourses(_ indices: Set<Int>, movingWeeksBy offset: Int, toDay day: Int?) throws {
        guard !indices.isEmpty, offset != 0 || day != nil else { throw ScheduleIssue.message("请选择课程和调整方式") }
        var updated = courses
        for i in indices {
            guard courses.indices.contains(i) else { throw ScheduleIssue.message("课程已变更，请重新选择") }
            let weeks = courses[i].weeks.map { $0 + offset }
            guard weeks.allSatisfy({ (1...dates.semesterTotalWeeks).contains($0) }) else {
                throw ScheduleIssue.message("调整后有课程超出本学期")
            }
            updated[i].weeks = weeks
            if let day { updated[i].day = day }
        }
        courses = updated
    }
}

extension AccountTimetables {
    func checkName(_ raw: String, except key: String? = nil) throws -> String {
        let name = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { throw ScheduleIssue.message("请输入课表名称") }
        guard !semesters.contains(where: { $0.key != key && $0.value.title == name }) else {
            throw ScheduleIssue.message("已有同名课表")
        }
        return name
    }
    mutating func create(name: String, dates: SchoolTermDates) throws {
        let title = try checkName(name)
        let key = UUID().uuidString
        semesters[key] = CampusTimetable(semester: title, dates: dates, courses: [], slots: TeachingSlot.campusDefaults,
                                         displayName: title)
        selected = key
    }
    mutating func delete(_ key: String) {
        semesters.removeValue(forKey: key)
        if selected == key { selected = sortedKeys.first }
    }
    var sortedKeys: [String] {
        semesters.keys.sorted {
            let a = semesters[$0]!.title, b = semesters[$1]!.title
            return a == b ? $0 < $1 : a.localizedStandardCompare(b) == .orderedDescending
        }
    }

    mutating func importCourses(_ draft: TimetableDraft, dates: SchoolTermDates, into target: String?) throws {
        if let target, semesters[target] == nil { throw ScheduleIssue.message("目标课表已删除") }
        let key = target ?? draft.semester
        let previous = semesters[key], sameTerm = previous?.semester == draft.semester
        var config = dates
        config.firstDayOfWeek = previous?.dates.firstDayOfWeek
        config.defaultClassDuration = previous?.dates.defaultClassDuration
        config.defaultBreakDuration = previous?.dates.defaultBreakDuration
        var colors = [String: Int]()
        for course in previous?.courses ?? [] { if let color = course.color { colors[course.name] = color } }
        let imported = draft.courses.map { course -> TimetableCourse in
            var result = course
            result.color = colors[course.name] ?? course.color
            return result
        }
        semesters[key] = CampusTimetable(semester: draft.semester, dates: config, courses: imported, slots: draft.slots,
            displayName: previous?.displayName, display: previous?.display, adjustments: sameTerm ? previous?.adjustments : nil,
            comboSchedule: sameTerm ? previous?.comboSchedule : nil)
        selected = key
    }
}
