import XCTest
@testable import XatuOne

@MainActor
final class SessionTests: XCTestCase {
    func testDataIsPartitionedByVerifiedSchoolRecord() throws {
        let first = #"bg.form.addInput(form,"ids","101");"#
        XCTAssertEqual(try CampusSession.accountKey(in:first), try CampusSession.accountKey(in:first))
        XCTAssertNotEqual(try CampusSession.accountKey(in:first),try CampusSession.accountKey(in:#"bg.form.addInput(form,"ids","102");"#))
        XCTAssertThrowsError(try CampusSession.accountKey(in:"<input type=password>"))
    }
    func testAcademicRequestsStayInCampusService() throws {
        XCTAssertEqual(try CampusSession.academicURL("homeExt!main.action").host,"jwgl2018.xatu.edu.cn")
        XCTAssertThrowsError(try CampusSession.academicURL("https://example.org/eams/"))
        XCTAssertThrowsError(try CampusSession.academicURL("/outside"))
    }
}
