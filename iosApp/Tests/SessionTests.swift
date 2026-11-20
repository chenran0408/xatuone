import XCTest
import WebKit
@testable import XatuOne

@MainActor
final class SessionTests: XCTestCase {
    @available(iOS 17.0, *)
    func testSavedProfilesReopenTheirOwnWebKitStoresAndLogoutOnlyClearsCurrent() async throws {
        let suite = UUID().uuidString
        let defaults = try XCTUnwrap(UserDefaults(suiteName:suite))
        defer { defaults.removePersistentDomain(forName:suite) }
        let first = UUID(), second = UUID()
        var directory = CampusProfiles(defaults:defaults,legacy:"")
        directory.remember(id:"test-a",storeID:first)
        directory.remember(id:"test-b",storeID:second)
        directory.rename(id:"test-a",to:"账号 A")
        directory.save(defaults)
        defaults.set("test-a",forKey:"xatu.eams.active-profile")
        let session = CampusSession(defaults:defaults)
        let cookie = try XCTUnwrap(HTTPCookie(properties:[.domain:"example.org",.path:"/",.name:"profile-test",.value:"A"]))
        let storeA = session.webView.configuration.websiteDataStore
        await storeA.httpCookieStore.setCookie(cookie)
        session.choose(directory.entries[1])
        XCTAssertEqual(session.webView.configuration.websiteDataStore.identifier,second)
        let cookiesB = await session.webView.configuration.websiteDataStore.httpCookieStore.allCookies()
        XCTAssertFalse(cookiesB.contains { $0.name == cookie.name })
        let reopened = CampusSession(defaults:defaults)
        XCTAssertEqual(reopened.accountID,"test-b")
        XCTAssertEqual(reopened.webView.configuration.websiteDataStore.identifier,second)
        try await reopened.logout()
        XCTAssertEqual(reopened.profiles.entries.map(\.label),["账号 A"])
        reopened.choose(directory.entries[0])
        let restored = await reopened.webView.configuration.websiteDataStore.httpCookieStore.allCookies()
        XCTAssertEqual(restored.first(where: { $0.name == cookie.name })?.value,"A")
        try await reopened.logout()
        let cleared = await storeA.httpCookieStore.allCookies()
        XCTAssertFalse(cleared.contains { $0.name == cookie.name })
    }

    func testExistingLoginKeepsItsDefaultStoreWhenUpdating() {
        let suite = UUID().uuidString
        let defaults = UserDefaults(suiteName:suite)!
        defer { defaults.removePersistentDomain(forName:suite) }
        let existing = CampusProfiles(defaults:defaults,legacy:"existing")
        XCTAssertEqual(existing.entries.map(\.id),["existing"])
        XCTAssertNil(existing.entries.first?.storeID)
        XCTAssertEqual(CampusProfiles(defaults:defaults,legacy:"existing").entries,existing.entries)
    }

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
    func testNetworkPlanCallbackKeepsEscapedQueryAndOnlyUpgradesExpectedGET() {
        var callback = URLRequest(url:URL(string:"http://self.xatu.edu.cn/Self/sso_login?service=a%2Fb&lang=zh")!)
        XCTAssertEqual(CampusSession.secureLoginRedirect(callback)?.absoluteString,
                       "https://self.xatu.edu.cn/Self/sso_login?service=a%2Fb&lang=zh")
        callback.httpMethod = "POST"
        XCTAssertNil(CampusSession.secureLoginRedirect(callback))
        let notice = URLRequest(url:URL(string:"http://self.xatu.edu.cn/Self/service/packageDisclaimer")!)
        XCTAssertEqual(CampusSession.secureLoginRedirect(notice)?.absoluteString,
                       "https://self.xatu.edu.cn/Self/service/packageDisclaimer")
        for url in ["http://self.xatu.edu.cn/other", "http://self.xatu.edu.cn:8080/Self/sso_login",
                    "http://other.example.org/Self/sso_login"] {
            XCTAssertNil(CampusSession.secureLoginRedirect(URLRequest(url:URL(string:url)!)))
        }
    }
}
