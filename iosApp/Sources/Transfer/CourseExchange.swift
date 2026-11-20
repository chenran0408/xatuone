import Foundation
import CryptoKit

enum CourseExchange {
    static func object(_ data: Data) throws -> [String: Any] {
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ScheduleIssue.message("文件不是课表")
        }
        return object
    }

    static func export(_ table: CampusTimetable) throws -> Data {
        var json = try object(JSONEncoder().encode(table))
        json["config"] = json.removeValue(forKey: "dates")
        json["timeSlots"] = json.removeValue(forKey: "slots")
        return try JSONSerialization.data(withJSONObject: json, options: [.prettyPrinted, .sortedKeys])
    }

    static func imported(_ data: Data, name: String, start: Date, weeks: Int, fallbackSlots: [TeachingSlot]) throws -> CampusTimetable {
        var json = try object(data)
        guard var courses = json["courses"] as? [[String: Any]] else { throw ScheduleIssue.message("文件中没有课程列表") }
        let slots: [TeachingSlot]
        if let raw = json["timeSlots"] as? [[String:Any]], !raw.isEmpty {
            slots = try JSONDecoder().decode([TeachingSlot].self, from: JSONSerialization.data(withJSONObject: raw))
        } else { slots = fallbackSlots }
        for index in courses.indices {
            // Android permits sections to be null for a course with explicit start and end times.
            if courses[index]["isCustomTime"] as? Bool == true,
               (courses[index]["startSection"] as? Int == nil || courses[index]["endSection"] as? Int == nil) {
                guard let a = courses[index]["customStartTime"] as? String, let b = courses[index]["customEndTime"] as? String,
                      let startMinute = TeachingSlot.minutes(a), let endMinute = TeachingSlot.minutes(b), startMinute < endMinute else {
                    throw ScheduleIssue.message("自定义课程时间无效")
                }
                let overlapping = slots.filter { $0.startTime < b && $0.endTime > a }
                let closest = overlapping.first ?? slots.min {
                    abs((TeachingSlot.minutes($0.startTime) ?? 0) - startMinute) <
                    abs((TeachingSlot.minutes($1.startTime) ?? 0) - startMinute)
                }
                courses[index]["startSection"] = closest?.number ?? 1
                courses[index]["endSection"] = overlapping.last?.number ?? closest?.number ?? 1
            }
        }
        json["semester"] = json["semester"] as? String ?? name
        json["displayName"] = name
        json["courses"] = courses
        var dates = json["config"] as? [String: Any] ?? [:]
        dates["semesterStartDate"] = SchoolCalendar.iso(start)
        dates["semesterTotalWeeks"] = weeks
        json["dates"] = dates
        json["slots"] = try JSONSerialization.jsonObject(with: JSONEncoder().encode(slots))
        let table = try JSONDecoder().decode(CampusTimetable.self, from: JSONSerialization.data(withJSONObject: json))
        try validate(table)
        return table
    }

    static func validate(_ table: CampusTimetable) throws {
        guard !table.semester.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              SchoolCalendar.date(table.dates.semesterStartDate) != nil, (1...53).contains(table.dates.semesterTotalWeeks) else {
            throw ScheduleIssue.message("请检查课表名称、开学日期和周数")
        }
        guard (1...7).contains(table.dates.firstDayOfWeek ?? 1),
              table.slots.isEmpty || TeachingSlot.valid(table.slots) else { throw ScheduleIssue.message("节次顺序或时间无效") }
        for rule in table.comboSchedule?.publicSchedules ?? [] {
            guard let first = SchoolCalendar.date(rule.startDate), let last = SchoolCalendar.date(rule.endDate), first <= last,
                  TeachingSlot.valid(rule.timeSlots) else { throw ScheduleIssue.message("组合作息的日期或节次无效") }
        }
        for (date, adjustment) in table.adjustments ?? [:] {
            guard SchoolCalendar.date(date) != nil, adjustment.lessonDate.map({SchoolCalendar.date($0) != nil}) != false else {
                throw ScheduleIssue.message("假期或调休日期无效")
            }
        }
        for c in table.courses {
            guard !c.name.isEmpty, (1...7).contains(c.day), c.startSection > 0, c.endSection >= c.startSection,
                  c.weeks.allSatisfy({ (1...table.dates.semesterTotalWeeks).contains($0) }),
                  c.credit.map({ $0.isFinite && $0 >= 0 }) != false else { throw ScheduleIssue.message("请检查课程、节次及上课周次") }
            if c.isCustomTime == true {
                guard let a = c.customStartTime.flatMap(CourseEvents.minutes), let b = c.customEndTime.flatMap(CourseEvents.minutes),
                      a < b else { throw ScheduleIssue.message("自定义课程时间无效") }
            }
        }
    }
}

struct CampusEvent: Equatable {
    let uid: String
    let name: String
    let place: String
    let teacher: String
    let start: Date
    let end: Date
}

enum CourseEvents {
    static func minutes(_ value: String) -> Int? {
        TeachingSlot.minutes(value)
    }

    static func key(_ parts: [String]) -> String {
        let value = parts.map { "\($0.utf8.count):\($0)" }.joined()
        return SHA256.hash(data: Data(value.utf8)).map { String(format:"%02x",$0) }.joined()
    }

    static func occurrences(_ table: CampusTimetable) throws -> [CampusEvent] {
        try CourseExchange.validate(table)
        let start = SchoolCalendar.date(table.dates.semesterStartDate)!
        let first = table.startOfWeek(containing: start)
        var result: [CampusEvent] = []
        for d in 0..<(table.dates.semesterTotalWeeks * 7) {
            let date = SchoolCalendar.add(d, to: first)
            var duplicates: [String:Int] = [:]
            for c in table.courses(on: date) {
                guard let time = table.time(for: c, on: date) else { throw ScheduleIssue.message("请先为「\(c.title)」设置节次时间") }
                let pair = time.components(separatedBy: "–")
                guard pair.count == 2, let a = minutes(pair[0]), let b = minutes(pair[1]), a < b else {
                    throw ScheduleIssue.message("「\(c.title)」的课程时间无效")
                }
                let id = key([table.semester, SchoolCalendar.iso(date), c.name, c.teacher, c.position, time])
                let index = duplicates[id,default:0]
                duplicates[id] = index + 1
                result.append(CampusEvent(uid:"\(id)-\(index)", name:c.name, place:c.position, teacher:c.teacher,
                                          start:date.addingTimeInterval(Double(a * 60)), end:date.addingTimeInterval(Double(b * 60))))
            }
        }
        return result
    }

    static func ics(_ events: [CampusEvent], alarm: Int?, now: Date = Date()) -> Data {
        let utc = DateFormatter()
        utc.locale = Locale(identifier:"en_US_POSIX")
        utc.timeZone = TimeZone(secondsFromGMT:0)
        utc.dateFormat = "yyyyMMdd'T'HHmmss'Z'"
        func escaped(_ text: String) -> String {
            text.replacingOccurrences(of:"\\",with:"\\\\").replacingOccurrences(of:"\r\n",with:"\n")
                .replacingOccurrences(of:"\r",with:"\n").replacingOccurrences(of:"\n",with:"\\n")
                .replacingOccurrences(of:",",with:"\\,").replacingOccurrences(of:";",with:"\\;")
        }
        func folded(_ text: String) -> String {
            var line = "", count = 0
            for scalar in text.unicodeScalars {
                let width = scalar.utf8.count
                if count + width > 75 { line += "\r\n "; count = 1 }
                line.unicodeScalars.append(scalar); count += width
            }
            return line
        }
        var lines = ["BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//XATU One//Campus Timetable//ZH","CALSCALE:GREGORIAN"]
        let stamp = utc.string(from:now)
        for e in events {
            lines += ["BEGIN:VEVENT","UID:\(e.uid)@xatuone","DTSTAMP:\(stamp)","DTSTART:\(utc.string(from:e.start))",
                      "DTEND:\(utc.string(from:e.end))","SUMMARY:\(escaped(e.name))","LOCATION:\(escaped(e.place))","DESCRIPTION:\(escaped(e.teacher))"]
            if let alarm { lines += ["BEGIN:VALARM","TRIGGER:-PT\(alarm)M","ACTION:DISPLAY","DESCRIPTION:\(escaped(e.name))","END:VALARM"] }
            lines.append("END:VEVENT")
        }
        lines.append("END:VCALENDAR")
        return Data((lines.map(folded).joined(separator:"\r\n") + "\r\n").utf8)
    }
}
