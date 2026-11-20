import XCTest
@testable import XatuOne

final class ScheduleTests: XCTestCase {
    private func entry() -> TimetableCourse {
        TimetableCourse(name: "测试课程 | 123.04", teacher: "教师", position: "教三",
                        day: 1, startSection: 3, endSection: 4, weeks: [1, 18], credit: 2.5,
                        isCustomTime: true, customStartTime: "10:10", customEndTime: "11:40")
    }
    private func table() -> CampusTimetable {
        CampusTimetable(semester: "2024-2025学年 秋季学期",
                        dates: SchoolTermDates(semesterStartDate: "2024-09-02", semesterTotalWeeks: 20),
                        courses: [entry()], slots: [TeachingSlot(number: 3, startTime: "10:20", endTime: "11:05"),
                                                    TeachingSlot(number: 4, startTime: "11:15", endTime: "12:00")])
    }

    func testDayAndWeekUseSchoolTimezoneAndActualCourseWeeks() throws {
        let table = table()
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        XCTAssertEqual(table.week(on: monday), 1)
        XCTAssertNil(table.week(on: SchoolCalendar.add(-1, to: monday)))
        XCTAssertEqual(table.week(on: monday.addingTimeInterval(7 * 86_400 - 1)), 1)
        XCTAssertEqual(table.week(on: SchoolCalendar.add(7, to: monday)), 2)
        XCTAssertEqual(table.courses(on: monday), [entry()])
        XCTAssertTrue(table.courses(on: SchoolCalendar.add(1, to: monday)).isEmpty)
        XCTAssertTrue(table.courses(on: SchoolCalendar.add(7, to: monday)).isEmpty)
        XCTAssertEqual(table.courses(on: SchoolCalendar.add(17 * 7, to: monday)), [entry()])
        XCTAssertEqual(table.time(for: entry()), "10:10–11:40")
        var normal = entry(); normal.isCustomTime = false
        XCTAssertEqual(table.time(for: normal), "10:20–12:00")
        XCTAssertEqual(entry().number, "123.04")
        XCTAssertNil(SchoolCalendar.date("2024-02-30"))
    }

    func testHistoricalSyncDoesNotReuseAnotherSemesterOrTruncateLaterClasses() {
        let table = table()
        var draft = TimetableDraft(semester: table.semester, courses: table.courses, slots: table.slots)
        XCTAssertEqual(draft.usableDates(previous: table), table.dates)
        var shortTerm = table
        shortTerm.dates.semesterTotalWeeks = 16
        XCTAssertNil(draft.usableDates(previous: shortTerm))
        draft.semester = "2025-2026学年 秋季学期"
        XCTAssertNil(draft.usableDates(previous: table))
        draft.config = SchoolTermDates(semesterStartDate: "2025-09-01", semesterTotalWeeks: 26)
        XCTAssertEqual(draft.usableDates(previous: table), draft.config)
    }

    func testAccountsRemainIsolatedAndAnEncodingFailurePreservesLastImport() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = TimetableStore(directory: directory)
        let first = String(repeating: "a", count: 64), second = String(repeating: "b", count: 64)
        let table = table(), initial = AccountTimetables(selected: table.semester, semesters: [table.semester: table])
        try store.save(initial, account: first)
        XCTAssertEqual(try store.load(account: first), initial)
        XCTAssertNil(try store.load(account: second).current)
        var invalid = table
        invalid.courses[0].credit = .infinity
        XCTAssertThrowsError(try store.save(AccountTimetables(selected: table.semester, semesters: [table.semester: invalid]), account: first))
        XCTAssertEqual(try store.load(account: first), initial)
    }
}
