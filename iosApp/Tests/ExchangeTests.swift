import XCTest
@testable import XatuOne

final class ExchangeTests: XCTestCase {
    private let sample = #"""
    {"courses":[{"name":"材料 | M1","teacher":"教师","position":"A101","day":1,"startSection":null,
      "endSection":null,"weeks":[1,2],"credit":2,"color":3,"remark":"实验","isCustomTime":true,
      "customStartTime":"09:00","customEndTime":"09:35"}],
      "timeSlots":[{"number":1,"startTime":"08:55","endTime":"09:40","alias":"第一节"}],
      "config":{"semesterStartDate":"2026-08-31","semesterTotalWeeks":2,"firstDayOfWeek":1},
      "comboSchedule":{"name":"秋季","publicSchedules":[{"name":"短作息","startDate":"2026-09-01",
        "endDate":"2026-09-08","timeSlots":[{"number":1,"startTime":"10:00","endTime":"10:45"}]}]}}
    """#

    private func imported() throws -> CampusTimetable {
        try CourseExchange.imported(Data(sample.utf8),name:"秋季",start:XCTUnwrap(SchoolCalendar.date("2026-08-31")),weeks:2,fallbackSlots:[])
    }

    func testAndroidJSONRoundTripPreservesCourseAndSeasonalTimes() throws {
        let first = try imported()
        let c = try XCTUnwrap(first.courses.first)
        XCTAssertEqual(c.number,"M1"); XCTAssertEqual(c.startSection,1); XCTAssertEqual(c.credit,2)
        XCTAssertEqual(c.color,3); XCTAssertEqual(c.remark,"实验")
        XCTAssertEqual(first.comboSchedule?.publicSchedules.first?.timeSlots.first?.startTime,"10:00")
        let bytes = try CourseExchange.export(first)
        let android = try CourseExchange.object(bytes)
        XCTAssertNotNil(android["timeSlots"]); XCTAssertNotNil(android["config"])
        let again = try CourseExchange.imported(bytes, name:"秋季",start:XCTUnwrap(SchoolCalendar.date("2026-08-31")),weeks:2,fallbackSlots:[])
        XCTAssertEqual(first,again)
    }
    func testExchangeRejectsCoursesOutsideChosenTerm() throws {
        XCTAssertThrowsError(try CourseExchange.imported(Data(sample.utf8),name:"秋季",start:Date(),weeks:1,fallbackSlots:[]))
    }
    func testICSMatchesMakeupDayAndSeasonalClockAndEscapesText() throws {
        var table = try imported()
        table.courses[0].isCustomTime = false
        table.courses[0].position = "A,楼;实验\n室"
        table.adjustments = ["2026-08-31": .init(label:"休息",lessonDate:nil),
                             "2026-09-01": .init(label:"补周一",lessonDate:"2026-08-31")]
        let events = try CourseEvents.occurrences(table)
        XCTAssertEqual(events.count,2); XCTAssertEqual(Set(events.map(\.uid)).count,2)
        XCTAssertEqual(events,try CourseEvents.occurrences(table))
        let result = String(decoding: CourseEvents.ics(events,alarm:10),as:UTF8.self)
        XCTAssertTrue(result.contains("DTSTART:20260901T020000Z"))
        XCTAssertFalse(result.contains("DTSTART:20260831"))
        XCTAssertTrue(result.contains(#"LOCATION:A\,楼\;实验\n室"#))
        XCTAssertTrue(result.contains("TRIGGER:-PT10M"))
        XCTAssertTrue(result.components(separatedBy:"\r\n").allSatisfy { $0.utf8.count <= 75 })
    }
    func testRestoreValidatesAllTablesAndIsScopedToDestinationOwner() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at:directory) }
        let store = TimetableStore(directory:directory)
        let defaults = try XCTUnwrap(UserDefaults(suiteName:UUID().uuidString))
        let t = try imported()
        let a = AccountTimetables(selected:"stable-id",semesters:["stable-id":t])
        defaults.set("floating",forKey:"xatu.appearance.style"); defaults.set("secret",forKey:"not-to-be-exported")
        let bytes = try JSONEncoder().encode(CampusBackup(archive:a,defaults:defaults))
        XCTAssertFalse(String(decoding:bytes,as:UTF8.self).contains("secret"))
        let backup = try CampusBackup.read(bytes); try backup.restore(owner:"0",store:store,defaults:defaults)
        XCTAssertEqual(try store.load(account:"0"),a); XCTAssertNil(try store.load(account:"aa").current)
        var broken = a; broken.semesters["stable-id"]?.courses[0].day = 8
        XCTAssertThrowsError(try CampusBackup.read(JSONEncoder().encode(CampusBackup(archive:broken,defaults:defaults))))
        XCTAssertEqual(try store.load(account:"0"),a)
    }
    func testDAVRequiresTLSAndCredentialsAreScopedToRequest() throws {
        let credentials = DAVCredential(address:"https://example.org/dav/backup.json",user:"student",password:"sample")
        let request = try credentials.request(method:"PUT")
        XCTAssertEqual(request.httpMethod,"PUT"); XCTAssertNil(request.url?.user)
        XCTAssertTrue(request.value(forHTTPHeaderField:"Authorization")?.hasPrefix("Basic ") == true)
        for url in ["http://example.org/dav/backup","https://example.org/dav/","https://user@example.org/backup"] {
            XCTAssertThrowsError(try DAVCredential(address:url).request(method:"GET"))
        }
    }
}
