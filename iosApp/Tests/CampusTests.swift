import XCTest
import CoreLocation
@testable import XatuOne

final class CampusTests: XCTestCase {
    func testGuestAndExpiredPaymentResponsesAreDistinctFromEmptyRoomData() throws {
        XCTAssertThrowsError(try CampusPowerAPI.confirmUser(["messageCode": 0, "data": NSNull()])) {
            XCTAssertEqual($0 as? PowerError, .login)
        }
        XCTAssertNoThrow(try CampusPowerAPI.confirmUser(["messageCode": "0", "data": ["id": 42]]))
        XCTAssertThrowsError(try CampusPowerAPI.remaining(["messageCode": 50014, "data": []])) {
            XCTAssertEqual($0 as? PowerError, .login)
        }
        XCTAssertThrowsError(try CampusPowerAPI.remaining(["messageCode": 0, "data": []])) {
            XCTAssertEqual($0 as? PowerError, .noReading)
        }
        XCTAssertThrowsError(try CampusPowerAPI.remaining(["messageCode": "99", "message": "服务暂停"])) {
            XCTAssertEqual($0 as? PowerError, .message("服务暂停"))
        }
    }

    func testBalanceUsesSchoolMeterSemanticsAndDecimalRounding() throws {
        func reading(_ row: [String: Any]) throws -> Decimal {
            try CampusPowerAPI.remaining(["messageCode": 0, "data": [row]])
        }
        XCTAssertEqual(try reading(["oddl": "71.5", "suml": "100"]), Decimal(string: "28.50"))
        XCTAssertEqual(try reading(["oddl": "0.005", "suml": NSNull()]), Decimal(string: "0.01"))
        XCTAssertEqual(try reading(["oddl": "101.25", "suml": "100"]), Decimal(string: "-1.25"))
        XCTAssertThrowsError(try reading(["oddl": "0.01度"]))
    }

    func testRoomSelectionUsesRealProjectAndParentIdentifiers() throws {
        let raw: [String: Any] = ["data": [
            ["id": "wifi", "projectName": "网费", "proModelUrl": "network"],
            ["id": 7, "projectName": "宿舍", "proModelUrl": "electricNewCapec"]
        ]]
        let projects = try CampusPowerAPI.options(raw, level: 0)
        XCTAssertEqual(projects, [PowerOption(id: "7", name: "宿舍")])
        let choices = projects + [PowerOption(id: "a", name: "校区"), PowerOption(id: "b", name: "楼栋")]
        let list = CampusPowerAPI.optionRequest(choices)
        XCTAssertTrue(list.path.hasSuffix("/queryRoomList"))
        XCTAssertEqual(list.form, ["projectId": "7", "areaid": "a", "buildid": "b"])
    }

    func testCachedReadingsAreIsolatedByAccountAndFullRoomHierarchy() throws {
        let suite = "campus-power-test-" + UUID().uuidString
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let project = PowerOption(id: "p", name: "项目")
        let area = PowerOption(id: "a", name: "校区")
        let roomNumber = PowerOption(id: "101", name: "101")
        let first = PowerRoom(project: project, area: area, building: PowerOption(id: "b1", name: "一号楼"), room: roomNumber)
        let second = PowerRoom(project: project, area: area, building: PowerOption(id: "b2", name: "二号楼"), room: roomNumber)
        let time = Date(timeIntervalSince1970: 1_000)
        var cache = CampusPowerHistory()
        cache.selected = first
        cache.record(PowerReading(time: time, amount: 20), for: first)
        cache.selected = second
        XCTAssertTrue(cache.readings.isEmpty)
        cache.record(PowerReading(time: time, amount: 8), for: second)
        try cache.save(account: "test-a", defaults: defaults)
        var restored = try CampusPowerHistory.load(account: "test-a", defaults: defaults)
        XCTAssertEqual(restored.readings.last?.amount, 8)
        restored.selected = first
        XCTAssertEqual(restored.readings.last?.amount, 20)
        XCTAssertTrue(try CampusPowerHistory.load(account: "test-b", defaults: defaults).readings.isEmpty)
    }

    func testDailyUsesLastCampusLocalReadingAndKeepsGapsOrTopUpsOutOfTheDecrease() throws {
        let value = PowerOption(id: "a", name: "A")
        let room = PowerRoom(project: value, area: value, building: value, room: value)
        let iso = ISO8601DateFormatter()
        var history = CampusPowerHistory()
        history.selected = room
        for (stamp, balance) in [("2026-11-10T15:59:00Z", 12), ("2026-11-10T16:00:00Z", 11),
                                 ("2026-11-11T08:00:00Z", 9), ("2026-11-13T00:00:00Z", 6),
                                 ("2026-11-14T00:00:00Z", 40)] {
            history.record(PowerReading(time: try XCTUnwrap(iso.date(from: stamp)), amount: Decimal(balance)), for: room)
        }
        XCTAssertEqual(history.daily.map(\.last.amount), [12, 9, 6, 40])
        XCTAssertEqual(history.daily.map(\.decrease), [nil, 3, nil, nil])
        XCTAssertEqual(history.recentChange, 34)
    }

    func testLogoutClearsOnlyTheOwnersSavedDeviceLocationAndPowerHistory() throws {
        let suite = "campus-checkin-" + UUID().uuidString
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let place = CheckInPlace(position: CLLocation(latitude: 34, longitude: 109))
        for owner in ["a", "b"] {
            try place.save(account: owner, defaults: defaults)
            try CampusPowerHistory().save(account: owner, defaults: defaults)
        }
        CampusPowerHistory.clear(account: "a", defaults: defaults)
        XCTAssertNil(try CheckInPlace.load(account: "a", defaults: defaults))
        XCTAssertNil(defaults.data(forKey: "xatu.power.v1.a"))
        XCTAssertEqual(try CheckInPlace.load(account: "b", defaults: defaults)?.latitude, 34)
        XCTAssertNotNil(defaults.data(forKey: "xatu.power.v1.b"))
    }
}
