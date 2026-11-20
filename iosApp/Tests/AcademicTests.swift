import XCTest
@testable import XatuOne

final class AcademicTests: XCTestCase {
    @MainActor func testHistorySummaryReadsLabeledTotalsAndKeepsAssessmentParts() async throws {
        let html = """
        <table><tr><th>学期</th><th>门数</th><th>总学分</th><th>平均绩点</th></tr>
        <tr><td>2023-2024学年</td><td>2</td><td>5</td><td>3.0</td></tr>
        <tr><td>在校汇总</td><td>4</td><td>10</td><td>3.40</td></tr></table>
        <table><tr><th>课程名称</th><th>学分</th><th>平时成绩</th><th>总评成绩</th><th>绩点</th></tr>
        <tr><td>课程甲</td><td>2</td><td>80</td><td>88</td><td>3.7</td></tr></table>
        """
        let page = try AcademicParser.gradeReport(await AcademicDocument.read(html))
        XCTAssertEqual(page.highlights.first { $0.label == "平均绩点" }?.value, "3.40")
        let row = try XCTUnwrap(page.sections.first?.rows.first)
        XCTAssertEqual(row.fields.first { $0.label == "成绩" }?.value, "88")
        XCTAssertEqual(row.fields.first { $0.label == "平时成绩" }?.value, "80")
    }

    @MainActor func testExamDateTimeSeatAndEmptyVersusUnexpectedPage() async throws {
        let html = """
        <table><tr><th>课程名称</th><th>日期</th><th>考试时间</th><th>考场</th><th>座位号</th></tr>
        <tr><td>课程甲</td><td>2026-12-10</td><td>09:00-11:00</td><td>教学楼</td><td>12</td></tr></table>
        """
        let doc = try await AcademicDocument.read(html)
        let row = try XCTUnwrap(AcademicParser.exams(doc).first)
        XCTAssertEqual(row.fields.first { $0.label == "考试时间" }?.value, "2026-12-10 09:00-11:00")
        XCTAssertEqual(row.fields.first { $0.label == "座位号" }?.value, "12")
        let empty = try await AcademicDocument.read("<p>暂无考试</p>")
        XCTAssertTrue(try AcademicParser.exams(empty).isEmpty)
        let invalid = try await AcademicDocument.read("<p>访问受限</p>")
        XCTAssertThrowsError(try AcademicParser.exams(invalid))
    }

    @MainActor func testStudyPlanAndCatalogRetainCourseFields() async throws {
        let plan = try await AcademicDocument.read("""
            <p>专业/专业方向：材料 GPA：3.20 要求学分/实修学分：150 / 58</p>
            <table><tr class="darkColumn"><td>通识</td><td>40</td><td>16</td></tr>
            <tr><td>1</td><td>A01</td><td>课程甲</td><td>2</td><td>2</td><td>88</td><td>是</td></tr></table>
            """)
        let progress = try AcademicParser.plan(plan)
        XCTAssertEqual(progress.highlights.first { $0.label == "实修学分" }?.value, "58")
        XCTAssertEqual(progress.sections.first?.rows.first?.title, "课程甲")
        let catalog = try await AcademicDocument.read("""
            <table><tr><td>A01</td><td class="course">001 课程甲</td><td>2</td><td>32</td>
            <td></td><td></td><td></td><td></td><td></td><td>秋季</td><td></td><td>是</td></tr></table>
            """)
        let row = try XCTUnwrap(AcademicParser.catalog(catalog).sections.first?.rows.first)
        XCTAssertEqual(row.title, "课程甲")
        XCTAssertEqual(row.fields.first { $0.label == "性质" }?.value, "必修")
    }

    func testDefaultSemesterUsesOfficialWeekInsteadOfNewestID() throws {
        let html = #"[{id:3,schoolYear:"2027-2028",name:"1"},{id:2,schoolYear:"2026-2027",name:"1"}]"#
        let terms = AcademicParser.semesters(html, options: [])
        let today = try XCTUnwrap(AcademicParser.schoolCalendar.date(from: .init(year: 2026, month: 11, day: 20)))
        let monday = try XCTUnwrap(AcademicParser.firstMonday(week: 12, today: today))
        XCTAssertEqual(AcademicParser.current(terms, start: monday)?.id, "2")
        XCTAssertEqual(AcademicParser.week("<span>12</span><i>/26</i><strong>当前教学周</strong>"), 12)
    }

    func testCalendarRangesRejectBadDatesAndKeepCrossYearTerms() {
        let text = "2026年8月31日—1月18日，2026年2月30日至7月1日，2026年10月1日至10月7日"
        XCTAssertEqual(AcademicCalendarPage.ranges(text), ["2026年8月31日 至 2027年1月18日"])
    }

    func testNextExamUsesFullSchoolTimeAndOnlyTheSignedInAccount() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let start = try XCTUnwrap(AcademicParser.schoolCalendar.date(from: DateComponents(year: 2026, month: 12, day: 10, hour: 9)))
        let now = start.addingTimeInterval(-86_400)
        func row(_ title: String, _ when: String) -> AcademicItem {
            AcademicItem(id: 0, title: title, fields: [AcademicField("考试时间", when), AcademicField("考场", "一号楼")])
        }
        let future = row("课程甲", "2026-12-10 周四 09:00–11:00")
        let incomplete = row("缺年份", "12-09 12:00")
        XCTAssertNil(incomplete.examStart)
        XCTAssertEqual(future.examCountdown(now: now), "距考试 1 天")
        XCTAssertEqual(future.examCountdown(now: start.addingTimeInterval(-60)), "今天考试")
        let stamp = now.addingTimeInterval(-600)
        func snapshot(_ rows: [AcademicItem]) -> AcademicSnapshot {
            AcademicSnapshot(report: AcademicReport(sections: [AcademicSection(id: 0, title: "考试", rows: rows)]), updatedAt: stamp)
        }
        try AcademicCache.update(account: "first", directory: directory) {
            $0.reports["exams"] = ["autumn": snapshot([incomplete, future]), "spring": snapshot([row("过去", "2026-06-05 11:00")])]
            $0.reports["grades"] = ["autumn": snapshot([row("其他服务", "2026-12-09 13:00")])]
        }
        try AcademicCache.update(account: "second", directory: directory) {
            $0.reports["exams"] = ["spring": snapshot([row("另一个账号", "2026-12-11 08:00")])]
        }
        let nearest = try XCTUnwrap(AcademicCache.nextExam(account: "first", now: now, directory: directory))
        XCTAssertEqual(nearest.title, "课程甲")
        XCTAssertEqual(nearest.start, start)
        XCTAssertEqual(nearest.updatedAt, stamp)
        XCTAssertEqual(nearest.semesterID, "autumn")
        XCTAssertEqual(nearest.room, "一号楼")
        XCTAssertNil(try AcademicCache.nextExam(account: "first", now: start.addingTimeInterval(60), directory: directory))
        try AcademicCache.clear(account: "first", directory: directory)
        XCTAssertTrue(try AcademicCache.load(account: "first", directory: directory).reports.isEmpty)
        XCTAssertEqual(try AcademicCache.nextExam(account: "second", now: now, directory: directory)?.title, "另一个账号")
    }

    @MainActor func testGradeSearchAndGPAParticipationDoNotInventPoints() async throws {
        let doc = try await AcademicDocument.read("""
            <table><tr><th>学期</th><th>课程代码</th><th>课程名称</th><th>学分</th><th>是否计入绩点</th><th>学分绩点</th></tr>
            <tr><td>2025秋季</td><td>PHY-A</td><td>物理</td><td>2</td><td>是</td><td>3.50</td></tr>
            <tr><td>2025秋季</td><td>PE</td><td>体育</td><td>1</td><td>否</td><td></td></tr></table>
            """)
        let report = try AcademicParser.gradeReport(doc)
        let row = try XCTUnwrap(report.sections.first?.rows.first)
        XCTAssertTrue(row.matches("phy-a")); XCTAssertTrue(row.matches("物理")); XCTAssertTrue(row.matches("秋季"))
        XCTAssertFalse(row.matches("2027"))
        XCTAssertEqual(row.value("是否计绩"), "是")
        XCTAssertEqual(row.value("绩点"), "3.50")
        XCTAssertNil(report.sections.first?.rows.last?.value("绩点"))
        XCTAssertFalse(report.highlights.contains { $0.label == "平均绩点" })
    }

    @MainActor func testStudyTotalsIncludeOnlyRootGroupsAndCatalogMatchesTheRightCode() async throws {
        let plan = try await AcademicDocument.read("""
            <table><tr class="darkColumn"><td>通识</td><td>40.5</td><td>16</td></tr>
            <tr class="darkColumn"><td>（一）外语</td><td>14</td><td>8</td></tr>
            <tr class="darkColumn"><td>1 英语</td><td>7</td><td>4</td></tr>
            <tr class="darkColumn"><td>实践</td><td>5</td><td>2</td></tr></table>
            """)
        let report = try AcademicParser.plan(plan)
        XCTAssertEqual(report.highlights.first { $0.label == "要求学分" }?.value, "45.5")
        XCTAssertEqual(report.highlights.first { $0.label == "实修学分" }?.value, "18")
        func course(_ code: String) -> AcademicItem {
            AcademicItem(id: 0, title: "同名课程", fields: [AcademicField("课程代码", code)])
        }
        let catalog = AcademicReport(sections: [AcademicSection(id: 0, title: "方案", rows: [course("A"), course("B")])])
        XCTAssertEqual(AcademicParser.catalogMatch(for: course("B"), in: catalog)?.value("课程代码"), "B")
        XCTAssertNil(AcademicParser.catalogMatch(for: course("C"), in: catalog))
    }

    func testPublicCalendarSavesAndReplacesOriginalWithItsOwnMetadataAndOCR() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let source = AcademicCalendarPage.index
        let saved = AcademicCalendarCopy(page: .init(title: "秋季", article: source, image: source, dateRanges: ["秋季学期"]),
                                        original: Data([9, 3]), updatedAt: Date(timeIntervalSince1970: 1_000), recognized: "识别文字")
        try saved.save(directory: directory)
        let restored = try XCTUnwrap(AcademicCalendarCopy.load(directory: directory))
        XCTAssertEqual(restored.original, saved.original)
        XCTAssertEqual(restored.page.title, "秋季")
        XCTAssertEqual(restored.recognized, "识别文字")
        XCTAssertEqual(restored.updatedAt, saved.updatedAt)
        let next = AcademicCalendarCopy(page: .init(title: "春季", article: source, image: source, dateRanges: []),
                                       original: Data([8]), updatedAt: Date(), recognized: nil)
        try next.save(directory: directory)
        let updated = try XCTUnwrap(AcademicCalendarCopy.load(directory: directory))
        XCTAssertEqual(updated.original, next.original)
        XCTAssertNil(updated.recognized)
    }
}
