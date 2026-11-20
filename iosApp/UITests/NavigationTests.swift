import XCTest

final class NavigationTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    private func launchSignedOut() -> XCUIApplication {
        let app = XCUIApplication()
        // Override the active profile for this process without deleting preferences or cookies.
        app.launchArguments = ["-xatu.eams.active-profile", ""]
        app.launch()
        return app
    }

    private func selectTab(_ label: String, in app: XCUIApplication) {
        let tab = app.tabBars.buttons[label]
        XCTAssertTrue(tab.waitForExistence(timeout: 8))
        tab.tap()
        XCTAssertTrue(tab.isSelected)
    }

    private func back(from detail: String, to parent: String, in app: XCUIApplication) {
        let bar = app.navigationBars[detail]
        // The leading navigation button works with both a localized Back label and a compact chevron.
        bar.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars[parent].waitForExistence(timeout: 8))
    }

    func testSignedOutCourseAndElectricityPaths() {
        let app = launchSignedOut()
        selectTab("课程", in: app)
        XCTAssertTrue(app.staticTexts["登录后查看课表"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.buttons["登录教务"].isHittable)

        selectTab("校园", in: app)
        app.buttons["宿舍电费"].tap()
        XCTAssertTrue(app.navigationBars["宿舍电费"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.buttons["登录教务账号"].isHittable)
        XCTAssertFalse(app.buttons["选择宿舍房间"].exists)
        back(from: "宿舍电费", to: "校园服务", in: app)

        app.buttons["校园签到"].tap()
        XCTAssertTrue(app.buttons["打开校园签到网页"].waitForExistence(timeout: 8))
        back(from: "校园签到", to: "校园服务", in: app)
        selectTab("课程", in: app)
        XCTAssertTrue(app.staticTexts["登录后查看课表"].waitForExistence(timeout: 8))
    }

    func testAcademicLinksOpenTheirOwnDetailAndReturn() {
        let app = launchSignedOut()
        selectTab("教务", in: app)
        for title in ["成绩与学分", "考试安排", "学业进度", "培养方案", "教学周"] {
            let link = app.buttons[title]
            XCTAssertTrue(link.waitForExistence(timeout: 8))
            link.tap()
            XCTAssertTrue(app.navigationBars[title].waitForExistence(timeout: 8))
            XCTAssertTrue(app.navigationBars[title].buttons["刷新"].exists)
            XCTAssertTrue(app.buttons["登录教务系统"].waitForExistence(timeout: 8))
            back(from: title, to: "教务", in: app)
        }
        XCTAssertTrue(app.buttons["校历"].exists)
    }

    func testLogoutConfirmationCanBeCancelled() {
        let app = launchSignedOut()
        selectTab("我的", in: app)
        XCTAssertTrue(app.buttons["登录教务系统"].waitForExistence(timeout: 8))
        app.buttons["退出并清除登录状态"].tap()
        let confirmation = app.buttons["退出登录"]
        XCTAssertTrue(confirmation.waitForExistence(timeout: 8))
        app.buttons["取消"].tap()
        let closed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: confirmation)
        XCTAssertEqual(XCTWaiter.wait(for: [closed], timeout: 8), .completed)
        XCTAssertTrue(app.buttons["登录教务系统"].isHittable)
    }

    func testCampusNetworkOpensThePlanServiceAndReturns() {
        let app = launchSignedOut()
        selectTab("校园", in: app)
        app.buttons["校园网套餐，网页服务"].tap()
        let bar = app.navigationBars["校园网套餐"]
        XCTAssertTrue(bar.waitForExistence(timeout: 8))
        bar.buttons["关闭"].tap()
        XCTAssertTrue(app.navigationBars["校园服务"].waitForExistence(timeout: 8))
    }
}
