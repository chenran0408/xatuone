import Foundation
import WebKit

struct AcademicItem: Identifiable, Codable {
    var id: Int
    let title: String
    let fields: [AcademicField]
    var summary: String { fields.prefix(2).map { "\($0.label) \($0.value)" }.joined(separator: " · ") }
}

struct AcademicField: Codable {
    let label: String
    let value: String
    init(_ label: String, _ value: String) { self.label = label; self.value = value }
}

struct AcademicSection: Identifiable, Codable {
    let id: Int
    let title: String
    var rows: [AcademicItem]
}

struct AcademicReport: Codable {
    var highlights: [AcademicField] = []
    var sections: [AcademicSection] = []
}

struct AcademicIssue: LocalizedError {
    let text: String
    init(_ text: String) { self.text = text }
    var errorDescription: String? { text }
}

struct AcademicSemester: Identifiable, Equatable, Codable {
    let id: String
    let year: String
    let term: String
    var startDate: String? = nil
    var title: String {
        let label = term == "1" ? "秋季学期" : term == "2" ? "春季学期" : term
        return year.isEmpty ? label : "\(year)学年 \(label)"
    }
}

struct AcademicDocument: Decodable {
    struct Cell: Decodable { let text: String; let course: Bool }
    struct Row: Decodable { let cells: [Cell]; let group: Bool; var values: [String] { cells.map(\.text) } }
    struct Link: Decodable { let text: String; let url: String }
    struct Picture: Decodable { let source: String; let original: String }
    struct Option: Decodable { let value: String; let title: String }
    let title: String
    let text: String
    let outsideTables: String
    let tables: [[Row]]
    let links: [Link]
    let images: [Picture]
    let options: [Option]
    let batches: [String]

    struct Table {
        let headers: [String]
        let rows: [[String]]
        func column(_ words: String...) -> Int? {
            headers.firstIndex { header in words.contains { header.contains($0) } }
        }
        func value(_ row: [String], _ index: Int?) -> String {
            guard let index, row.indices.contains(index) else { return "" }
            return row[index]
        }
    }
    func table(_ matches: ([String]) -> Bool) -> Table? {
        for rows in tables {
            guard let header = rows.firstIndex(where: { matches($0.values) }) else { continue }
            return Table(headers: rows[header].values, rows: rows.dropFirst(header + 1).map(\.values))
        }
        return nil
    }
    var knownEmpty: Bool {
        ["没有考试安排", "暂无考试", "没有查询到", "未查询到", "没有符合条件", "暂无成绩", "无符合条件", "暂无数据", "没有数据", "无数据"].contains { text.contains($0) }
    }

    // Parse into an inert template. Markup and page scripts never enter the active document.
    static let script = #"""
    const t = document.createElement('template'); t.innerHTML = html;
    const root = t.content, all = (el, s) => Array.from(el.querySelectorAll(s));
    const clean = s => (s || '').replace(/[\s\u00a0]+/g, ' ').trim();
    const text = node => clean(node.textContent);
    const rows = table => Array.from(table.rows).filter(r => r.closest('table') === table)
      .map(r => ({group:r.classList.contains('darkColumn'), cells:Array.from(r.cells).map(c => ({text:text(c), course:c.classList.contains('course')}))}));
    const result = {title:clean(root.querySelector('title')?.textContent), text:'',
      tables:all(root, 'table').map(rows),
      links:all(root,'a[href]').map(a=>({text:text(a),url:a.getAttribute('href')})),
      images:all(root,'img[src]').map(i=>({source:i.getAttribute('src'),original:i.getAttribute('orisrc')||''})),
      options:all(root,'[val],option[value]').map(o=>({value:o.getAttribute('val')||o.getAttribute('value')||'',title:text(o)})),
      batches:all(root,'select[name="examBatch.id"] option[value]').map(o=>o.value)};
    all(root,'style,script').forEach(el=>el.remove());
    function blockText(node) {
      return clean(Array.from(node.childNodes).map(n=>n.nodeType===3?n.nodeValue:blockText(n)).join(' '));
    }
    result.text = blockText(root);
    all(root,'table').forEach(el=>el.remove()); result.outsideTables = blockText(root);
    return JSON.stringify(result);
    """#

    @MainActor private static let engine: WKWebView = {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .nonPersistent()
        return WKWebView(frame: .zero, configuration: config)
    }()

    @MainActor static func read(_ html: String) async throws -> Self {
        let result = try await engine.callAsyncJavaScript(script, arguments: ["html": html], in: nil, contentWorld: .defaultClient)
        guard let json = result as? String else { throw AcademicIssue("教务页面解析失败") }
        return try JSONDecoder().decode(Self.self, from: Data(json.utf8))
    }
}

enum AcademicParser {
    static func captures(_ pattern: String, in text: String) -> [[String]] {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let range = NSRange(text.startIndex..., in: text)
        return regex.matches(in: text, range: range).map { match in
            (0..<match.numberOfRanges).map { index in
                Range(match.range(at: index), in: text).map { String(text[$0]) } ?? ""
            }
        }
    }

    static func week(_ html: String) -> Int? {
        let patterns = [#"<span>\s*(\d{1,2})\s*</span>\s*<i>\s*/\s*\d+\s*</i>[\s\S]{0,80}?当前教学周"#,
                        #"当前教学周[\s\S]{0,80}?<span>\s*(\d{1,2})\s*</span>"#, #"当前教学周\s*[:：]?\s*(\d{1,2})"#]
        return patterns.lazy.compactMap { captures($0, in: html).first.flatMap { Int($0[1]) } }.first
    }

    static var schoolCalendar: Calendar {
        var result = Calendar(identifier: .gregorian)
        result.timeZone = TimeZone(identifier: "Asia/Shanghai")!
        return result
    }

    static func firstMonday(week: Int, today: Date) -> Date? {
        guard week > 0 else { return nil }
        let cal = schoolCalendar
        let days = (cal.component(.weekday, from: today) + 5) % 7 + (week - 1) * 7
        return cal.date(byAdding: .day, value: -days, to: cal.startOfDay(for: today))
    }

    static func current(_ terms: [AcademicSemester], start: Date) -> AcademicSemester? {
        let c = schoolCalendar
        let autumn = c.component(.month, from: start) >= 7
        let year = c.component(.year, from: start) - (autumn ? 0 : 1)
        return terms.first { term in
            term.title.hasPrefix("\(year)-\(year + 1)学年") &&
            (autumn ? ["1", "秋季学期"] : ["2", "春季学期"]).contains(term.term)
        }
    }

    static func semesters(_ html: String, options: [AcademicDocument.Option]) -> [AcademicSemester] {
        var seen = Set<String>()
        var result = captures(#"\{[^{}]*\}"#, in: html).compactMap { object -> AcademicSemester? in
            func field(_ keys: [String]) -> String {
                for key in keys {
                    let pattern = #"["']?\#(key)["']?\s*:\s*(?:["']([^"']*)["']|(\d+))"#
                    if let m = captures(pattern, in: object[0]).first { return m[1].isEmpty ? m[2] : m[1] }
                }
                return ""
            }
            let id = field(["id"]), year = field(["schoolYear", "school_year", "academicYear", "year"])
            let name = field(["name", "termName", "term", "title"])
            guard Int(id) != nil, !name.isEmpty || !year.isEmpty, seen.insert(id).inserted else { return nil }
            let start = field(["startDate", "start_date", "beginDate", "start"])
            return AcademicSemester(id: id, year: year, term: name, startDate: start.isEmpty ? nil : start)
        }
        if result.isEmpty {
            result = options.compactMap { o in
                guard Int(o.value) != nil, !o.title.isEmpty, seen.insert(o.value).inserted else { return nil }
                let parts = o.title.components(separatedBy: "学年")
                return AcademicSemester(id: o.value, year: parts.count == 2 ? parts[0] : "",
                                        term: (parts.last ?? "").trimmingCharacters(in: .whitespaces))
            }
        }
        return result.sorted { (Int($0.id) ?? 0) > (Int($1.id) ?? 0) }
    }

    static func gradeReport(_ doc: AcademicDocument) throws -> AcademicReport {
        var report = AcademicReport()
        if let totals = doc.table({ $0.contains("门数") && $0.contains("总学分") && $0.contains("平均绩点") }),
           let row = totals.rows.first(where: { $0.first == "在校汇总" }) {
            report.highlights = ["门数", "总学分", "平均绩点"].map { AcademicField($0, totals.value(row, totals.headers.firstIndex(of: $0))) }
        } else {
            report.highlights = [("门数", "课程门数|课程数|门数"), ("总学分", "总学分|合计学分|学分合计|已修学分"),
                                 ("平均绩点", "平均学分绩点|平均绩点|平均GPA")].compactMap { label, pattern in
                captures("(?:\(pattern))[:：\\s]*([0-9]+(?:\\.[0-9]+)?)", in: doc.outsideTables).first.map { AcademicField(label, $0[1]) }
            }
        }
        guard let table = doc.table({ h in
            h.contains { $0.contains("课程名称") || $0.contains("科目") } &&
            (h.contains { $0.contains("成绩") || $0.contains("绩点") } ||
             (h.contains { ["学期","学年学期"].contains($0) } && h.contains { $0.contains("学分") }))
        }) else {
            if doc.knownEmpty { return report }
            throw AcademicIssue("未识别到成绩表")
        }
        let score = table.column("总评", "最终") ?? table.headers.firstIndex { ["成绩", "总成绩"].contains($0) }
        let credits = table.headers.firstIndex { $0.contains("学分") && !$0.contains("绩点") }
        let points = table.headers.firstIndex { ["绩点", "学分绩点", "课程绩点"].contains($0) }
        let counted = table.headers.firstIndex { ["是否计绩", "是否计入绩点", "是否计算绩点"].contains($0) }
        let rows = table.rows.enumerated().compactMap { index, row -> AcademicItem? in
            let title = table.value(row, table.column("课程名称", "科目"))
            guard !title.isEmpty, !title.contains("无数据") else { return nil }
            var fields = [AcademicField("成绩", table.value(row, score)), AcademicField("学分", table.value(row, credits)),
                          AcademicField("绩点", table.value(row, points)), AcademicField("是否计绩", table.value(row, counted)),
                          AcademicField("课程序号", table.value(row, table.column("课程序号", "课程代码", "课程号"))),
                          AcademicField("学期", table.value(row, table.column("学年学期", "学期", "学年")))]
            for (i, h) in table.headers.enumerated() where i != score && (h.contains("成绩") || h == "其他") {
                fields.append(AcademicField(h, table.value(row, i)))
            }
            return AcademicItem(id: index, title: title, fields: fields.filter { !$0.value.isEmpty })
        }
        report.sections = [AcademicSection(id: 0, title: "课程成绩", rows: rows)]
        return report
    }

    static func exams(_ doc: AcademicDocument) throws -> [AcademicItem] {
        guard let t = doc.table({ h in
            h.contains { $0.contains("课程名称") || $0.contains("科目") } &&
            h.contains { $0.contains("时间") || $0.contains("日期") || $0.contains("考场") }
        }) else {
            if doc.knownEmpty { return [] }
            throw AcademicIssue("未识别到考试表")
        }
        return t.rows.enumerated().compactMap { i, row in
            func get(_ index: Int?) -> String { t.value(row, index) }
            let name = get(t.column("课程名称", "科目")), date = get(t.column("日期")), time = get(t.column("考试时间", "时间"))
            let room = get(t.column("考场", "教室", "地点"))
            guard !name.isEmpty, !date.isEmpty || !time.isEmpty || !room.isEmpty else { return nil }
            let whenText = time.contains(date) ? time : [date, time].filter { !$0.isEmpty }.joined(separator: " ")
            return AcademicItem(id: i, title: name, fields: [
                AcademicField("考试时间", whenText), AcademicField("考场", room), AcademicField("座位号", get(t.column("座位"))),
                AcademicField("考试名称", get(t.column("考试名称"))), AcademicField("课程序号", get(t.column("课程序号", "课程代码", "课程号"))),
                AcademicField("备注", get(t.column("备注", "说明")))
            ].filter { !$0.value.isEmpty })
        }
    }

    static func plan(_ doc: AcademicDocument) throws -> AcademicReport {
        var groups: [AcademicSection] = []
        let text = doc.text
        var highlights = [("姓名", #"姓名\s*[:：]\s*(\S+)"#), ("专业", #"专业/专业方向\s*[:：]\s*(\S+)"#),
                          ("绩点", #"GPA\s*[:：]\s*([\d.]+)"#)].compactMap { label, p in
            captures(p, in: text).first.map { AcademicField(label, $0[1]) }
        }
        if let totals = captures(#"要求学分/实修学分\s*[:：]\s*([\d.]+)\s*/\s*([\d.]+)"#, in: text).first {
            highlights += [AcademicField("要求学分", totals[1]), AcademicField("实修学分", totals[2])]
        }
        var rootCreditPairs: [[Decimal]] = []
        var hasRoot = false, hasChild = false
        for row in doc.tables.flatMap({ $0 }) {
            let v = row.values
            if row.group, let title = v.first, !title.isEmpty {
                let numbers = v.dropFirst().filter { Double($0) != nil }
                if hasRoot, title.range(of: #"^[（(][一二三四五六七八九十]+[）)]"#, options: .regularExpression) != nil {
                    hasChild = true
                } else if !(hasChild && title.range(of: #"^\d+[\s　]"#, options: .regularExpression) != nil) {
                    hasRoot = true; hasChild = false
                    rootCreditPairs.append(numbers.prefix(2).compactMap { Decimal(string: $0, locale: Locale(identifier: "en_US_POSIX")) })
                }
                var heading = title
                if numbers.count >= 2 { heading += " · \(numbers[1]) / \(numbers[0]) 学分" }
                groups.append(AcademicSection(id: groups.count, title: heading, rows: []))
            } else if v.count >= 7, Int(v[0]) != nil, !v[2].isEmpty, let last = groups.indices.last {
                let fields = zip(["课程代码", "要求学分", "实修学分", "成绩", "是否通过"], [v[1],v[3],v[4],v[5],v[6]])
                let item = AcademicItem(id: groups[last].rows.count, title: v[2], fields: fields.compactMap { label, value in
                    value.isEmpty ? nil : AcademicField(label, value)
                })
                groups[last].rows.append(item)
            }
        }
        guard !groups.isEmpty else { throw AcademicIssue("未识别到学业完成情况") }
        if !highlights.contains(where: { $0.label == "要求学分" }), !rootCreditPairs.isEmpty,
           rootCreditPairs.allSatisfy({ $0.count == 2 }) {
            let required = rootCreditPairs.reduce(Decimal.zero) { $0 + $1[0] }
            let earned = rootCreditPairs.reduce(Decimal.zero) { $0 + $1[1] }
            highlights += [AcademicField("要求学分", NSDecimalNumber(decimal: required).stringValue),
                           AcademicField("实修学分", NSDecimalNumber(decimal: earned).stringValue)]
        }
        return AcademicReport(highlights: highlights, sections: groups)
    }

    static func catalogMatch(for course: AcademicItem, in report: AcademicReport) -> AcademicItem? {
        let rows = report.sections.flatMap(\.rows)
        if let code = course.value("课程代码"),
           let exact = rows.first(where: { $0.value("课程代码") == code }) { return exact }
        return rows.first { $0.title == course.title && (course.value("课程代码") == nil || $0.value("课程代码") == nil) }
    }

    static func catalog(_ doc: AcademicDocument) throws -> AcademicReport {
        var seen = Set<String>(), items: [AcademicItem] = []
        for row in doc.tables.flatMap({ $0 }) {
            guard let i = row.cells.firstIndex(where: \.course) else { continue }
            let v = row.values
            func at(_ offset: Int) -> String { v.indices.contains(i + offset) ? v[i + offset] : "" }
            guard Double(at(1)) != nil, seen.insert(at(-1).isEmpty ? at(0) : at(-1)).inserted else { continue }
            let name = at(0).replacingOccurrences(of: #"^\d+\s+"#, with: "", options: .regularExpression)
            let nature = at(10).contains("是") ? "必修" : at(10).contains("否") ? "选修" : ""
            items.append(AcademicItem(id: items.count, title: name, fields: [
                AcademicField("学分", at(1)), AcademicField("学时", at(2)), AcademicField("课程代码", at(-1)),
                AcademicField("学期", at(8)), AcademicField("性质", nature)
            ].filter { !$0.value.isEmpty }))
        }
        guard !items.isEmpty else { throw AcademicIssue("未识别到培养方案课程") }
        return AcademicReport(sections: [AcademicSection(id: 0, title: "培养方案课程", rows: items)])
    }
}
