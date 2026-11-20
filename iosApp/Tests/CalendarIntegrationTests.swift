import EventKit
import XCTest
@testable import XatuOne

final class CalendarIntegrationTests: XCTestCase {
    private struct SavedEvent: Hashable {
        let id: String
        let title: String
        let url: URL?
        let start: Date
    }

    @MainActor
    func testRepeatedSyncMoveAndDeletePreserveUnmanagedEvents() async throws {
        #if targetEnvironment(simulator)
        let env = ProcessInfo.processInfo.environment
        guard let device = env["XATU_EVENTKIT_CI_DEVICE"], !device.isEmpty else {
            throw XCTSkip("EventKit writes require the explicitly selected disposable CI simulator.")
        }
        guard env["SIMULATOR_UDID"]?.lowercased() == device.lowercased() else {
            XCTFail("The active simulator does not match the dedicated EventKit test device.")
            return
        }
        let status = EKEventStore.authorizationStatus(for: .event)
        let hasAccess: Bool
        if #available(iOS 17, *) { hasAccess = status == .fullAccess }
        else { hasAccess = status == .authorized }
        guard hasAccess else {
            XCTFail("The CI simulator must grant full calendar permission before running this test.")
            return
        }

        let owner = UUID().uuidString
        let tableKey = UUID().uuidString
        let key = "xatu.calendar." + CourseEvents.key([owner, tableKey])
        let monday = SchoolCalendar.monday(of: Date())
        let first = TimetableCourse(name: "模拟课程甲", teacher: "", position: "", day: 1,
                                    startSection: 1, endSection: 1, weeks: [1])
        var removed = first
        removed.name = "模拟课程乙"
        removed.day = 2
        var table = CampusTimetable(semester: tableKey,
                                    dates: SchoolTermDates(semesterStartDate: SchoolCalendar.iso(monday), semesterTotalWeeks: 1),
                                    courses: [first, removed], slots: [TeachingSlot(number: 1, startTime: "09:00", endTime: "09:45")])
        let defaults = UserDefaults.standard
        defer {
            let store = EKEventStore()
            if let id = defaults.string(forKey: key), let calendar = store.calendar(withIdentifier: id),
               calendar.title == "一校通 · " + tableKey {
                XCTAssertNoThrow(try store.removeCalendar(calendar, commit: true))
            }
            defaults.removeObject(forKey: key)
            defaults.removeObject(forKey: key + ".start")
        }
        let sync = CourseCalendar()
        func write(_ value: CampusTimetable) async throws -> Int {
            try await sync.sync(value, owner: owner, tableKey: tableKey, alarm: nil) { true }
        }
        let initialCount = try await write(table)
        XCTAssertEqual(initialCount, 2)
        let id = try XCTUnwrap(defaults.string(forKey: key))
        func savedEvents() throws -> [SavedEvent] {
            let reader = EKEventStore()
            let calendar = try XCTUnwrap(reader.calendar(withIdentifier: id))
            let range = reader.predicateForEvents(withStart: monday, end: SchoolCalendar.add(7, to: monday), calendars: [calendar])
            return reader.events(matching: range).map {
                SavedEvent(id: $0.calendarItemIdentifier, title: $0.title, url: $0.url, start: $0.startDate)
            }
        }
        let initial = try savedEvents()
        XCTAssertEqual(initial.count, 2)
        XCTAssertEqual(Set(initial.map(\.title)), [first.name, removed.name])
        XCTAssertTrue(initial.allSatisfy { $0.url?.scheme == "xatuone-course" })

        let userStore = EKEventStore()
        let calendar = try XCTUnwrap(userStore.calendar(withIdentifier: id))
        // One ordinary event has the deleted course's title; another has a different binding.
        for url in [nil, URL(string: "xatuone-course://another-binding/keep")] as [URL?] {
            let event = EKEvent(eventStore: userStore)
            event.calendar = calendar
            event.title = removed.name
            event.startDate = SchoolCalendar.add(1, to: monday).addingTimeInterval(9 * 3600)
            event.endDate = event.startDate.addingTimeInterval(45 * 60)
            event.url = url
            try userStore.save(event, span: .thisEvent, commit: true)
        }
        let all = try savedEvents()
        let userEvents = Set(all).subtracting(initial)
        XCTAssertEqual(all.count, 4)
        XCTAssertEqual(userEvents.count, 2)

        let repeatedCount = try await write(table)
        XCTAssertEqual(repeatedCount, 2)
        XCTAssertEqual(defaults.string(forKey: key), id)
        let repeated = try savedEvents()
        XCTAssertEqual(repeated.count, 4)
        XCTAssertEqual(Set(repeated), Set(all))

        var moved = first
        moved.day = 3
        table.courses = [moved]
        let changedCount = try await write(table)
        XCTAssertEqual(changedCount, 1)
        let changed = try savedEvents()
        XCTAssertEqual(changed.count, 3)
        XCTAssertTrue(userEvents.isSubset(of: Set(changed)))
        let updated = try XCTUnwrap(Set(changed).subtracting(userEvents).first)
        XCTAssertEqual(updated.title, first.name)
        XCTAssertEqual(updated.start, SchoolCalendar.add(2, to: monday).addingTimeInterval(9 * 3600))
        XCTAssertTrue(Set(initial).isDisjoint(with: changed))

        table.courses = []
        let clearedCount = try await write(table)
        XCTAssertEqual(clearedCount, 0)
        let remaining = try savedEvents()
        XCTAssertEqual(remaining.count, 2)
        XCTAssertEqual(Set(remaining), userEvents)
        #else
        throw XCTSkip("Calendar integration writes run only on the dedicated iPhone simulator.")
        #endif
    }
}
