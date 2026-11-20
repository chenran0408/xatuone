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
}
