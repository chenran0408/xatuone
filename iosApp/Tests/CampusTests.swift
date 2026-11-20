import XCTest
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
}
