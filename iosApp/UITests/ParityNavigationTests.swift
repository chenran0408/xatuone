import XCTest

final class ParityNavigationTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    private func launchGuest() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-xatu.eams.active-profile", "", "-xatu.start.tab", "home"]
        app.launch()
        return app
    }

    private func tap(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(element.waitForExistence(timeout: 8), file: file, line: line)
        element.tap()
    }

    private func tab(_ key: String, title: String, app: XCUIApplication) -> XCUIElement {
        let custom = app.buttons["campus.tab." + key]
        return custom.exists ? custom : app.tabBars.buttons[title]
    }

    private func back(_ title: String, app: XCUIApplication) {
        tap(app.navigationBars[title].buttons.element(boundBy: 0))
    }

    func testHomeElectricityLoginCanCloseBackToItsSource() {
        let app = launchGuest()
        XCTAssertTrue(app.navigationBars["首页"].waitForExistence(timeout: 8))
        tap(app.buttons["宿舍电费"])
        tap(app.buttons["登录教务账号"])
        let login = app.navigationBars["教务登录"]
        XCTAssertTrue(login.waitForExistence(timeout: 8))
        XCTAssertTrue(login.buttons["完成登录"].exists)
        tap(login.buttons["关闭"])
        XCTAssertTrue(app.buttons["登录教务账号"].waitForExistence(timeout: 8))
        back("宿舍电费", app: app)
        XCTAssertTrue(app.navigationBars["首页"].waitForExistence(timeout: 8))
        XCTAssertTrue(tab("home", title: "首页", app: app).isSelected)
    }

    func testAppearanceChangesKeepAccountTabAndCurrentPage() {
        let app = launchGuest()
        tap(tab("account", title: "我的", app: app))
        tap(app.buttons["外观与启动"])
        let page = app.navigationBars["外观与启动"]
        let style = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "界面样式")).firstMatch
        for name in ["紧凑标签栏", "悬浮导航", "iOS 系统"] {
            tap(style)
            tap(app.buttons[name])
            XCTAssertTrue(page.waitForExistence(timeout: 8))
            let active = name == "iOS 系统" ? app.tabBars.buttons["我的"] : app.buttons["campus.tab.account"]
            XCTAssertTrue(active.waitForExistence(timeout: 8))
            XCTAssertTrue(active.isSelected)
            let selected = (style.value as? String ?? "") + style.label
            XCTAssertTrue(selected.contains(name), "选中的样式应显示为 \(name)，实际为 \(selected)")
        }
        back("外观与启动", app: app)
        XCTAssertTrue(app.navigationBars["我的"].waitForExistence(timeout: 8))
    }

    func testGuestCanCreateTimetableShowCourseNumberAndReadCourseDetails() {
        let app = launchGuest()
        let suffix = UUID().uuidString.prefix(8)
        let table = "UIT-\(suffix)"
        let course = "Lab-\(suffix)"
        let number = "2401.02"
        tap(tab("courses", title: "课程", app: app))
        tap(app.navigationBars["课程表"].buttons["课表设置"])
        tap(app.buttons["课表管理"])
        tap(app.buttons["新建课表"])
        let title = app.textFields["课表名称"]
        tap(title)
        title.typeText(table)
        let tableMenu = app.buttons["\(table)的操作"]
        // Only remove this test's UUID-named table, including when a later assertion fails.
        addTeardownBlock {
            app.terminate()
            app.launch()
            self.tap(self.tab("courses", title: "课程", app: app))
            self.tap(app.navigationBars["课程表"].buttons["课表设置"])
            self.tap(app.buttons["课表管理"])
            if tableMenu.waitForExistence(timeout: 3) {
                let available = XCTNSPredicateExpectation(predicate: NSPredicate(format: "hittable == true"), object: tableMenu)
                XCTAssertEqual(XCTWaiter.wait(for: [available], timeout: 8), .completed)
                tableMenu.tap()
                self.tap(app.buttons["删除课表"])
                let prompt = app.staticTexts["删除「\(table)」及其中的课程？"]
                XCTAssertTrue(prompt.waitForExistence(timeout: 8))
                self.tap(app.buttons["删除课表"])
                let gone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: tableMenu)
                XCTAssertEqual(XCTWaiter.wait(for: [gone], timeout: 8), .completed)
            }
        }
        tap(app.navigationBars["新建课表"].buttons["保存"])
        XCTAssertTrue(tableMenu.waitForExistence(timeout: 8))
        back("课表管理", app: app)
        let numberSwitch = app.switches["课程序号"]
        XCTAssertTrue(numberSwitch.waitForExistence(timeout: 8))
        XCTAssertEqual(numberSwitch.value as? String, "0")
        numberSwitch.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)).tap()
        let enabled = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", "1"), object: numberSwitch)
        XCTAssertEqual(XCTWaiter.wait(for: [enabled], timeout: 5), .completed)
        tap(app.buttons["课程管理"])
        tap(app.buttons["新增课程"])
        let nameField = app.textFields["课程名称"]
        tap(nameField)
        nameField.typeText(course)
        let numberField = app.textFields["课程序号"]
        tap(numberField)
        numberField.typeText(number)
        tap(app.navigationBars["新增课程"].buttons["保存"])
        let row = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", course)).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 8))
        back("课程管理", app: app)
        back("课表设置", app: app)
        tap(app.segmentedControls.buttons["周课表"])
        let block = app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", course, number)).firstMatch
        tap(block)
        XCTAssertTrue(app.navigationBars["课程详情"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.staticTexts[course].exists)
        tap(app.navigationBars["课程详情"].buttons["完成"])
        XCTAssertTrue(block.waitForExistence(timeout: 8))
    }
}
