import XCTest
import WebKit
@testable import XatuOne

@MainActor
final class SessionTests: XCTestCase {
    func testOnlyVerifiedPaymentOriginCanCompleteLogin() {
        XCTAssertTrue(CampusSession.isPaymentOrigin(URL(string: "https://jfpay.xatu.edu.cn:443/casLogin/")))
        for address in ["http://jfpay.xatu.edu.cn/", "https://jfpay.xatu.edu.cn:8443/",
                        "https://authserver.xatu.edu.cn/", "https://jfpay.xatu.edu.cn.example.org/"] {
            XCTAssertFalse(CampusSession.isPaymentOrigin(URL(string: address)))
        }
        XCTAssertThrowsError(try CampusPowerAPI.confirmUser(["messageCode": "50014", "data": ["id": 42]])) {
            XCTAssertEqual($0 as? PowerError, .login)
        }
    }

    func testAccountHandoffClearsPaymentTokensAndKeepsAcademicCookies() async throws {
        let dataStore = WKWebsiteDataStore.nonPersistent()
        let cookies = dataStore.httpCookieStore
        let token = "datalook_reimbursement_token"
        for (name, domain, path) in [
            (token, ".xatu.edu.cn", "/api/"), ("paySession", "jfpay.xatu.edu.cn", "/"),
            ("casSession", ".xatu.edu.cn", "/"), ("eamsSession", "jwgl2018.xatu.edu.cn", "/eams/"),
            (token, "other.example.org", "/")
        ] {
            let cookie = try XCTUnwrap(HTTPCookie(properties: [
                .name: name, .domain: domain, .path: path, .value: "test"
            ]))
            await cookies.setCookie(cookie)
        }
        let initial = await cookies.allCookies()
        XCTAssertEqual(initial.count, 5)
        try await CampusSession.clearPaymentCookies(in: cookies)
        let retained = await cookies.allCookies()
        XCTAssertEqual(Set(retained.map(\.name)), ["casSession", "eamsSession", token])
        let tokenDomain = retained.first { $0.name == token }?.domain.trimmingCharacters(in: CharacterSet(charactersIn: "."))
        XCTAssertEqual(tokenDomain, "other.example.org")
    }

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
