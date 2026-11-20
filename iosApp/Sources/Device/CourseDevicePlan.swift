import Foundation

enum CourseDevicePlan {
    static let reminderLimit = 48

    static func agenda(for table: CampusTimetable, now: Date) -> CourseWidgetData {
        var courses: [DeviceCourse] = []
        if let opening = SchoolCalendar.date(table.dates.semesterStartDate) {
            let first = max(opening, SchoolCalendar.calendar.startOfDay(for: now))
            let last = SchoolCalendar.add(7 * min(max(table.dates.semesterTotalWeeks, 0), 53),
                                          to: table.startOfWeek(containing: opening))
            var day = first
            while day < last {
                let dayCourses = table.courses(on: day).map { course -> DeviceCourse in
                    let times = table.time(for: course, on: day)?.components(separatedBy: "–") ?? []
                    let start = times.first.flatMap { time($0, on: day) }
                    let end = times.count == 2 ? time(times[1], on: day) : nil
                    return DeviceCourse(day: day, start: start, end: end, title: course.title,
                                        room: course.position, sections: course.sectionText)
                }
                courses += dayCourses.sorted { ($0.start ?? .distantFuture) < ($1.start ?? .distantFuture) }
                day = SchoolCalendar.add(1, to: day)
            }
        }
        return CourseWidgetData(semester: table.semester, courses: courses)
    }

    static func time(_ text: String, on day: Date) -> Date? {
        let fields = text.split(separator: ":", omittingEmptySubsequences: false)
        guard fields.count == 2, let hour = Int(fields[0]), let minute = Int(fields[1]),
              (0..<24).contains(hour), (0..<60).contains(minute) else { return nil }
        return SchoolCalendar.calendar.date(bySettingHour: hour, minute: minute, second: 0, of: day)
    }

    static func reminders(in agenda: CourseWidgetData, now: Date, minutes: Int) -> [(Date, DeviceCourse)] {
        Array(agenda.courses.compactMap { course -> (Date, DeviceCourse)? in
            guard let trigger = course.start?.addingTimeInterval(-Double(minutes) * 60), trigger > now else { return nil }
            return (trigger, course)
        }.prefix(reminderLimit))
    }
}
