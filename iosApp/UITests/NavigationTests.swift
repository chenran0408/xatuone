import XCTest

final class NavigationTests: XCTestCase {
    func testStudentCanOpenServicesAndAccount() {
        let app = XCUIApplication()
        app.launch()
        let tabs = app.tabBars
        for name in ["教务","校园","我的","课程"] {
            let tab = tabs.buttons[name]
            XCTAssertTrue(tab.waitForExistence(timeout:8))
            tab.tap()
            XCTAssertTrue(tab.isSelected)
        }
        tabs.buttons["我的"].tap()
        XCTAssertTrue(app.buttons["登录教务系统"].exists)
    }
}
