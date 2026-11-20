import EventKit
import Foundation

@MainActor
final class CourseCalendar {
    private let store = EKEventStore()

    func sync(_ table: CampusTimetable, owner: String, tableKey: String, alarm: Int?, active: () -> Bool) async throws -> Int {
        let entries = try CourseEvents.occurrences(table)
        let allowed: Bool
        if #available(iOS 17, *) { allowed = try await store.requestFullAccessToEvents() }
        else { allowed = try await store.requestAccess(to: .event) }
        guard allowed else { throw ScheduleIssue.message("请在系统设置中允许访问日历") }
        guard active() else { throw CancellationError() }
        let defaults = UserDefaults.standard
        let key = "xatu.calendar." + CourseEvents.key([owner,tableKey])
        let calendar: EKCalendar
        if let id = defaults.string(forKey:key), let saved = store.calendar(withIdentifier:id) {
            calendar = saved
        } else {
            guard !entries.isEmpty else { return 0 }
            guard let source = store.defaultCalendarForNewEvents?.source else { throw ScheduleIssue.message("系统中没有可写入的日历") }
            calendar = EKCalendar(for:.event,eventStore:store)
            calendar.source = source
            calendar.title = "一校通 · " + table.title
            try store.saveCalendar(calendar,commit:true)
            defaults.set(calendar.calendarIdentifier,forKey:key)
        }
        guard calendar.allowsContentModifications else { throw ScheduleIssue.message("该日历不可写入") }
        // Scope updates to this account/table's own events, preserving entries the user added.
        let termStart = SchoolCalendar.date(table.dates.semesterStartDate)!
        let ranges = [termStart, defaults.object(forKey:key + ".start") as? Date].compactMap { $0 }
        var managed: [String:EKEvent] = [:]
        let marker = String(key.dropFirst("xatu.calendar.".count))
        do {
            for date in Set(ranges) {
                let predicate = store.predicateForEvents(withStart:SchoolCalendar.add(-7,to:date),
                                                         end:SchoolCalendar.add(374,to:date),calendars:[calendar])
                for event in store.events(matching:predicate) {
                    if let u = event.url, u.scheme == "xatuone-course", u.host == marker { managed[u.lastPathComponent] = event }
                }
            }
            for e in entries {
                let event = managed.removeValue(forKey:e.uid) ?? EKEvent(eventStore:store)
                event.calendar = calendar
                event.url = URL(string:"xatuone-course://\(marker)/\(e.uid)")
                event.title = e.name; event.location = e.place; event.notes = e.teacher
                event.startDate = e.start; event.endDate = e.end
                event.timeZone = SchoolCalendar.calendar.timeZone
                event.alarms = alarm.map { [EKAlarm(relativeOffset:Double(-60 * $0))] } ?? []
                try store.save(event,span:.thisEvent,commit:false)
            }
            for obsolete in managed.values { try store.remove(obsolete,span:.thisEvent,commit:false) }
            try store.commit()
            defaults.set(termStart, forKey:key + ".start")
        } catch { store.reset(); throw error }
        return entries.count
    }
}
