import XCTest
@testable import XatuOne

final class DeviceCourseTests: XCTestCase {
    @MainActor
    func testWidgetAndHostUseTheInstalledAppsIdentifierForSharing() throws {
        let app = Bundle.main.bundleURL
        let widget = app.appendingPathComponent("PlugIns/CourseWidget.appex")
        let identifier = try XCTUnwrap(Bundle.main.bundleIdentifier)
        XCTAssertEqual(CourseWidgetExchange.hostIdentifier(for: app), identifier)
        XCTAssertEqual(CourseWidgetExchange.hostIdentifier(for: widget), identifier)
    }

    func testInterruptedAccountSwitchRetriesNotificationCleanupAfterRelaunch() throws {
        let suite = UUID().uuidString
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let owner = CourseNotificationOwner(defaults: defaults)
        owner.finish("first")
        XCTAssertFalse(owner.beginSwitch(from: nil, to: "first"))
        XCTAssertTrue(owner.beginSwitch(from: "first", to: "second"))
        // The process stopped before the notification queue finished removing the first account.
        let relaunched = CourseNotificationOwner(defaults: defaults)
        XCTAssertTrue(relaunched.beginSwitch(from: nil, to: "second"))
        relaunched.finish("second")
        XCTAssertFalse(relaunched.beginSwitch(from: nil, to: "second"))
    }

    private func fixture() -> CampusTimetable {
        let course = TimetableCourse(name: "自习", teacher: "", position: "A区", day: 1,
                                     startSection: 1, endSection: 1, weeks: [1, 2])
        let morning = TeachingSlot(number: 1, startTime: "08:00", endTime: "08:45")
        return CampusTimetable(semester: "本地课表", dates: SchoolTermDates(semesterStartDate: "2024-09-02",
                            semesterTotalWeeks: 2), courses: [course], slots: [morning])
    }

    func testAdjustedDateUsesActualDaysTimetableAndNextCourse() throws {
        var table = fixture()
        table.adjustments = ["2024-09-02": TimetableDayAdjustment(label: "停课"),
                             "2024-09-03": TimetableDayAdjustment(label: "补课", lessonDate: "2024-09-02")]
        let slot = TeachingSlot(number: 1, startTime: "10:00", endTime: "10:45")
        table.comboSchedule = TimetableComboSchedule(publicSchedules: [
            TimetableScheduleRule(name: "短课", startDate: "2024-09-03", endDate: "2024-09-03", timeSlots: [slot])
        ])
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        let tuesday = SchoolCalendar.add(1, to: monday)
        let agenda = CourseDevicePlan.agenda(for: table, now: monday)
        XCTAssertTrue(agenda.today(at: monday).isEmpty)
        XCTAssertEqual(agenda.today(at: tuesday).map(\.title), ["自习"])
        XCTAssertEqual(agenda.next(at: monday)?.start, CourseDevicePlan.time("10:00", on: tuesday))
        let reminders = CourseDevicePlan.reminders(in: agenda, now: monday, minutes: 10)
        XCTAssertEqual(reminders.first?.0, CourseDevicePlan.time("09:50", on: tuesday))
        XCTAssertEqual(agenda.next(at: try XCTUnwrap(reminders.first?.1.start))?.day, SchoolCalendar.add(7, to: monday))
    }

    func testMissingSlotsAndCustomTimesDoNotProduceInventedNotificationTimes() throws {
        var table = fixture()
        table.slots = []
        var early = table.courses[0]
        early.name = "早课"
        early.isCustomTime = true
        early.customStartTime = "00:05"
        early.customEndTime = "00:50"
        table.courses.append(early)
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        let sunday = SchoolCalendar.add(-1, to: monday)
        let agenda = CourseDevicePlan.agenda(for: table, now: sunday)
        XCTAssertEqual(agenda.today(at: monday).count, 2)
        XCTAssertNil(agenda.today(at: monday).last?.start)
        let triggers = CourseDevicePlan.reminders(in: agenda, now: sunday, minutes: 10)
        XCTAssertEqual(triggers.count, 2)
        XCTAssertEqual(triggers.first?.0, CourseDevicePlan.time("23:55", on: sunday))
        XCTAssertEqual(CourseDevicePlan.reminders(in: agenda, now: monday, minutes: 10).count, 1)
    }

    func testOnlyNearest48RequestsUseFutureTimes() throws {
        var table = fixture()
        table.dates.semesterTotalWeeks = 28
        table.courses = (1...5).map { weekday in
            var course = fixture().courses[0]
            course.day = weekday
            course.weeks = Array(1...28)
            return course
        }
        let first = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        let now = try XCTUnwrap(CourseDevicePlan.time("08:01", on: first))
        let result = CourseDevicePlan.reminders(in: CourseDevicePlan.agenda(for: table, now: now), now: now, minutes: 0)
        XCTAssertEqual(result.count, 48)
        XCTAssertTrue(result.allSatisfy { $0.0 > now })
        XCTAssertEqual(result.map { $0.0 }, result.map { $0.0 }.sorted())
        XCTAssertEqual(result.first?.1.day, SchoolCalendar.add(1, to: first))
    }

    func testTemporaryTransferIncludesTwoDaysAndOnlyOneSubsequentLesson() throws {
        let monday = try XCTUnwrap(SchoolCalendar.date("2024-09-02"))
        var table = fixture()
        table.semester = "private timetable title"
        table.courses = (1...5).map { day in
            var course = fixture().courses[0]
            course.day = day
            return course
        }
        let agenda = CourseDevicePlan.agenda(for: table, now: monday)
        let copied = agenda.temporarySnapshot(at: monday)
        XCTAssertEqual(copied.courses.map(\.day), (0...2).map { SchoolCalendar.add($0, to: monday) })
        XCTAssertEqual(copied.expires, SchoolCalendar.add(2, to: monday))
        XCTAssertNil(agenda.expires)
        XCTAssertFalse(String(decoding: try JSONEncoder().encode(copied), as: UTF8.self).contains(table.semester))
    }
}
