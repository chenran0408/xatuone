import XCTest
@testable import XatuOne

final class ScheduleEditingTests: XCTestCase {
    private let term = SchoolTermDates(semesterStartDate: "2024-09-02", semesterTotalWeeks: 20)
    private let course = TimetableCourse(name: "力学 | 001", teacher: "教师", position: "实验楼", day: 1,
                                         startSection: 1, endSection: 2, weeks: [1, 3])

    func testOldArchiveStillHasReadableTableAndDefaultDisplay() throws {
        let old = #"{"selected":"秋季","semesters":{"秋季":{"semester":"秋季","dates":{"semesterStartDate":"2024-09-02","semesterTotalWeeks":20},"courses":[{"name":"力学 | 001","teacher":"教师","position":"实验楼","day":1,"startSection":1,"endSection":2,"weeks":[1,3]}],"slots":[{"number":1,"startTime":"08:20","endTime":"09:05"},{"number":2,"startTime":"09:15","endTime":"10:00"}]}}}"#
        let archive = try JSONDecoder().decode(AccountTimetables.self, from: Data(old.utf8))
        let table = try XCTUnwrap(archive.current)
        XCTAssertEqual(table.title, "秋季")
        XCTAssertEqual(table.courses, [course])
        XCTAssertEqual(table.displayOptions, TimetableDisplay(showNumber: false, showTeacher: true, showLocation: true))
        XCTAssertEqual(table.time(for: course), "08:20–10:00")
    }

    func testGuestOwnsItsTablesSeparatelyAndRenameKeepsSelection() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = TimetableStore(directory: directory)
        var archive = AccountTimetables()
        try archive.create(name: " 草稿 ", dates: term)
        let first = try XCTUnwrap(archive.selected)
        XCTAssertThrowsError(try archive.create(name: "草稿", dates: term))
        try archive.create(name: "第二张", dates: term)
        let second = try XCTUnwrap(archive.selected)
        let name = try archive.checkName("实验课", except: first)
        archive.semesters[first]?.displayName = name
        XCTAssertEqual(archive.current?.title, "第二张")
        archive.delete(second)
        XCTAssertEqual(archive.selected, first)
        XCTAssertEqual(archive.current?.title, "实验课")
        try store.save(archive, account: "0")
        XCTAssertEqual(try store.load(account: "0"), archive)
        XCTAssertNil(try store.load(account: String(repeating: "a", count: 64)).current)
    }

    func testBulkChangeCannotLeaveAPartiallyShiftedSchedule() throws {
        var finalWeek = course
        finalWeek.weeks = [20]
        var table = CampusTimetable(semester: "秋季", dates: term, courses: [course, finalWeek], slots: TeachingSlot.campusDefaults)
        XCTAssertThrowsError(try table.adjustCourses([0, 1], movingWeeksBy: 1, toDay: 5))
        XCTAssertEqual(table.courses, [course, finalWeek])
        try table.adjustCourses([0], movingWeeksBy: 1, toDay: 5)
        XCTAssertEqual(table.courses[0].weeks, [2, 4])
        XCTAssertEqual(table.courses[0].day, 5)
        XCTAssertEqual(table.courses[1], finalWeek)
        XCTAssertEqual(try TimetableCourse.parseWeeks("1-3，3、5", limit: 5), [1, 2, 3, 5])
        XCTAssertThrowsError(try TimetableCourse.parseWeeks("3-1", limit: 5))
        XCTAssertThrowsError(try TimetableCourse.parseWeeks("1,6", limit: 5))
    }

    func testMakeupUsesOriginalLessonButActualDaysTimeAndCustomOverride() throws {
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        let saturday = SchoolCalendar.add(5, to: monday)
        var table = CampusTimetable(semester: "秋季", dates: term, courses: [course], slots: TeachingSlot.campusDefaults)
        table.adjustments = ["2024-09-02": TimetableDayAdjustment(label: "放假"),
                             "2024-09-07": TimetableDayAdjustment(label: "补课", lessonDate: "2024-09-02")]
        table.comboSchedule = TimetableComboSchedule(publicSchedules: [
            TimetableScheduleRule(name: "周末", startDate: "2024-09-07", endDate: "2024-09-07",
                                  timeSlots: [TeachingSlot(number: 1, startTime: "08:10", endTime: "08:55")])])
        XCTAssertTrue(table.courses(on: monday).isEmpty)
        XCTAssertEqual(table.courses(on: saturday), [course])
        XCTAssertEqual(table.coursesInWeek(containing: monday)[5], [course])
        XCTAssertEqual(table.time(for: course, on: saturday), "08:10–10:00")
        XCTAssertEqual(table.time(for: course, on: monday), "08:20–10:00")
        let days = (0..<7).map { SchoolCalendar.add($0, to: monday) }
        let axis = table.weekPeriods(days: days, coursesByDay: table.coursesInWeek(containing: monday))
        XCTAssertNil(axis.first?.commonSlot)
        XCTAssertEqual(axis[1].commonSlot?.startTime, "09:15")
        var special = course
        special.isCustomTime = true; special.customStartTime = "10:10"; special.customEndTime = "11:40"
        XCTAssertEqual(table.time(for: special, on: saturday), "10:10–11:40")
        special.startSection = 13; special.endSection = 13
        table.courses.append(special)
        let updated = table.weekPeriods(days: days, coursesByDay: table.coursesInWeek(containing: monday))
        XCTAssertEqual(updated.last?.number, 13)
        XCTAssertNil(updated.last?.commonSlot)
    }

    func testResyncTargetPreservesItsPresentationAndOnlySameTermCalendar() throws {
        var decorated = course
        decorated.color = 5
        var old = CampusTimetable(semester: "秋季", dates: term, courses: [decorated], slots: TeachingSlot.campusDefaults,
                                  displayName: "我的课表", display: TimetableDisplay(showNumber: true))
        old.dates.firstDayOfWeek = 7
        old.adjustments = ["2024-09-02": TimetableDayAdjustment(label: "放假")]
        old.comboSchedule = TimetableComboSchedule(name: "分日期作息", publicSchedules: [])
        let other = CampusTimetable(semester: "其他", dates: term, courses: [], slots: TeachingSlot.campusDefaults)
        var archive = AccountTimetables(selected: "target", semesters: ["target": old, "other": other])
        var draft = TimetableDraft(semester: "秋季", courses: [course], slots: TeachingSlot.campusDefaults)
        try archive.importCourses(draft, dates: term, into: "target")
        XCTAssertEqual(archive.current?.adjustments, old.adjustments)
        XCTAssertEqual(archive.current?.comboSchedule, old.comboSchedule)
        XCTAssertEqual(archive.current?.courses.first?.color, 5)
        draft.semester = "春季"
        let spring = SchoolTermDates(semesterStartDate: "2025-02-17", semesterTotalWeeks: 18)
        try archive.importCourses(draft, dates: spring, into: "target")
        let saved = try XCTUnwrap(archive.current)
        XCTAssertEqual(saved.title, "我的课表")
        XCTAssertEqual(saved.semester, "春季")
        XCTAssertEqual(saved.dates.semesterStartDate, "2025-02-17")
        XCTAssertEqual(saved.dates.firstDayOfWeek, 7)
        XCTAssertEqual(saved.display, old.display)
        XCTAssertNil(saved.adjustments)
        XCTAssertNil(saved.comboSchedule)
        XCTAssertEqual(archive.semesters["other"], other)
        XCTAssertThrowsError(try archive.importCourses(draft, dates: spring, into: "removed"))
        XCTAssertEqual(archive.current, saved)
    }

    func testAutomaticSyncCannotOverwriteRepurposedSemesterTable() throws {
        var archive = AccountTimetables()
        var autumn = TimetableDraft(semester: "2024-2025学年 秋季学期", courses: [course], slots: TeachingSlot.campusDefaults)
        var spring = autumn
        spring.semester = "2024-2025学年 春季学期"
        let springDates = SchoolTermDates(semesterStartDate: "2025-02-17", semesterTotalWeeks: 20)
        try archive.importCourses(autumn, dates: term, into: nil)
        let initial = try XCTUnwrap(archive.selected)
        try archive.importCourses(spring, dates: springDates, into: initial)
        let replaced = try XCTUnwrap(archive.current)
        XCTAssertNil(archive.automaticTarget(for: autumn.semester))
        try archive.importCourses(autumn, dates: term, into: nil)
        let alternate = try XCTUnwrap(archive.selected)
        XCTAssertNotEqual(alternate, initial)
        autumn.courses[0].position = "新教室"
        try archive.importCourses(autumn, dates: term, into: nil)
        XCTAssertEqual(archive.selected, alternate)
        XCTAssertEqual(archive.automaticTarget(for: autumn.semester), alternate)
        XCTAssertEqual(archive.semesters.count, 2)
        XCTAssertEqual(archive.semesters[initial], replaced)
        XCTAssertEqual(archive.current?.courses.first?.position, "新教室")
    }

    func testHistoricalSyncUsesTheConfiguredTermDatesEvenAfterEditingTable() {
        let autumn = TimetableDraft(semester: "2024-2025学年 秋季学期", courses: [course], slots: TeachingSlot.campusDefaults)
        var table = CampusTimetable(semester: "手动课表", dates: term, courses: [], slots: TeachingSlot.campusDefaults)
        XCTAssertEqual(autumn.usableDates(previous: table), term)
        table.semester = autumn.semester
        table.dates.semesterStartDate = "2025-02-17"
        XCTAssertNil(autumn.matchingPrevious(table))
        XCTAssertNil(autumn.usableDates(previous: table))
    }

    func testSingleDateMovesKeepAllOtherWeeksAndCourseDetails() throws {
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        let tuesday = SchoolCalendar.add(1, to: monday)
        var from = course
        from.color = 4; from.credit = 2; from.remark = "实验"; from.isCustomTime = true
        from.customStartTime = "09:00"; from.customEndTime = "10:30"
        var to = course
        to.name = "高数"; to.day = 2; to.weeks = [1, 2]
        let original = CampusTimetable(semester: "秋季", dates: term, courses: [from, to], slots: TeachingSlot.campusDefaults)
        for mode in DayCourseMove.allCases {
            var table = original
            try table.moveCourses(from: monday, to: tuesday, mode: mode)
            var moved = from
            moved.day = 2; moved.weeks = [1]
            let atTarget = table.courses(on: tuesday)
            XCTAssertTrue(atTarget.contains(moved))
            XCTAssertEqual(atTarget.count, mode == .merge ? 2 : 1)
            if mode == .exchange {
                var exchanged = to
                exchanged.day = 1; exchanged.weeks = [1]
                XCTAssertEqual(table.courses(on: monday), [exchanged])
            } else { XCTAssertTrue(table.courses(on: monday).isEmpty) }
            XCTAssertEqual(table.courses(on: SchoolCalendar.add(14, to: monday)).map(\.name), [from.name])
            XCTAssertEqual(table.courses(on: SchoolCalendar.add(7, to: tuesday)).map(\.name), [to.name])
        }
    }

    func testExchangeAcrossWeeksAndOverwriteFromEmptyDateOnlyTouchThoseOccurrences() throws {
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        let thirdMonday = SchoolCalendar.add(14, to: monday)
        var recurring = course
        recurring.weeks = [1, 2, 3]
        var extra = course
        extra.name = "讨论课"; extra.weeks = [1]
        var table = CampusTimetable(semester: "秋季", dates: term, courses: [recurring, extra], slots: TeachingSlot.campusDefaults)
        try table.moveCourses(from: monday, to: thirdMonday, mode: .exchange)
        XCTAssertEqual(table.courses(on: monday).map(\.name), [recurring.name])
        XCTAssertEqual(Set(table.courses(on: thirdMonday).map(\.name)), [recurring.name, extra.name])
        XCTAssertEqual(table.courses(on: SchoolCalendar.add(7, to: monday)).map(\.name), [recurring.name])
        try table.moveCourses(from: SchoolCalendar.add(1, to: monday), to: thirdMonday, mode: .overwrite)
        XCTAssertTrue(table.courses(on: thirdMonday).isEmpty)
        let before = table
        XCTAssertThrowsError(try table.moveCourses(from: monday.addingTimeInterval(3600), to: monday, mode: .exchange))
        XCTAssertThrowsError(try table.moveCourses(from: monday, to: SchoolCalendar.add(-1, to: monday), mode: .overwrite))
        XCTAssertEqual(table, before)
    }
}
