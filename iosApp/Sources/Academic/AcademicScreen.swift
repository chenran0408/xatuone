import SwiftUI

private enum AcademicService: String, CaseIterable, Identifiable {
    case grades = "成绩与学分", exams = "考试安排", plan = "学业进度", catalog = "培养方案", week = "教学周"
    var id: Self { self }
    var icon: String {
        switch self {
        case .grades: return "list.bullet.rectangle"
        case .exams: return "calendar.badge.clock"
        case .plan: return "chart.bar"
        case .catalog: return "books.vertical"
        case .week: return "calendar"
        }
    }
    var usesTerm: Bool { self == .grades || self == .exams }
}

struct AcademicScreen: View {
    @EnvironmentObject private var session: CampusSession

    var body: some View {
        List {
            ForEach(AcademicService.allCases) { service in
                NavigationLink {
                    AcademicServiceScreen(service: service).id(session.accountID)
                } label: { Label(service.rawValue, systemImage: service.icon) }
            }
            NavigationLink { AcademicCalendarScreen() } label: { Label("校历", systemImage: "photo.on.rectangle") }
        }
        .navigationTitle("教务")
    }
}

private struct AcademicQuery: Hashable {
    let term: String
    let revision: Int
}

private struct AcademicServiceScreen: View {
    let service: AcademicService
    @EnvironmentObject private var session: CampusSession
    @State private var terms: [AcademicSemester] = []
    @State private var selected = ""
    @State private var report: AcademicReport?
    @State private var loading = false
    @State private var error: String?
    @State private var needsLogin = false
    @State private var revision = 0

    var body: some View {
        List {
            if service.usesTerm && (!terms.isEmpty || service == .grades) {
                Picker("学期", selection: $selected) {
                    Text("选择学期").tag("")
                    if service == .grades { Text("全部历史成绩").tag("history") }
                    ForEach(terms) { Text($0.title).tag($0.id) }
                }
            }
            if loading {
                ProgressView("正在查询")
            } else if let error {
                Text(error).foregroundStyle(.secondary)
                if needsLogin { Button("登录教务系统") { session.openAcademic() } }
                Button("重新查询") { revision += 1 }
            } else if let report {
                if !report.highlights.isEmpty {
                    Section("汇总") { fields(report.highlights) }
                }
                if report.sections.allSatisfy({ $0.rows.isEmpty }) && report.highlights.isEmpty && service != .plan {
                    Text(service == .exams ? "本学期暂无考试安排" : "暂无课程成绩").foregroundStyle(.secondary)
                }
                ForEach(report.sections) { group in
                    if !group.rows.isEmpty || service == .plan {
                        Section {
                            if group.rows.isEmpty { Text(group.title) }
                            ForEach(group.rows) { row in
                                if row.fields.isEmpty {
                                    Text(row.title)
                                } else {
                                    NavigationLink {
                                        List { fields(row.fields) }.navigationTitle(row.title)
                                    } label: {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(row.title)
                                            if !row.summary.isEmpty { Text(row.summary).font(.subheadline).foregroundStyle(.secondary) }
                                        }
                                    }
                                }
                            }
                        } header: { if !group.rows.isEmpty { Text(group.title) } }
                    }
                }
            }
        }
        .navigationTitle(service.rawValue)
        .toolbar { Button { revision += 1 } label: { Label("刷新", systemImage: "arrow.clockwise") }.disabled(loading) }
        .onChange(of: selected) { _ in report = nil; error = nil }
        .onChange(of: session.browser?.id) { page in
            if page == nil && needsLogin { revision += 1 }
        }
        .task(id: AcademicQuery(term: selected, revision: revision)) { await load() }
    }

    @ViewBuilder func fields(_ values: [AcademicField]) -> some View {
        ForEach(Array(values.enumerated()), id: \.offset) { _, field in
            LabeledContent(field.label) { Text(field.value).textSelection(.enabled) }
        }
    }

    @MainActor private func load() async {
        let term = selected
        loading = true; error = nil; needsLogin = false; report = nil
        do {
            try await session.ensureAcademic()
            if service.usesTerm && term.isEmpty {
                let html = try await session.academicHTML("dataQuery.action?tagId=&dataType=semesterCalendar", form: nil)
                let options = try await AcademicDocument.read(html).options
                let available = AcademicParser.semesters(html, options: options)
                guard !available.isEmpty else { throw AcademicIssue("教务系统未返回学期") }
                try Task.checkCancellation()
                terms = available
                let home = try await session.academicHTML("homeExt!main.action", form: nil)
                try Task.checkCancellation()
                // A user may pick a term while the home page is loading.
                if let week = AcademicParser.week(home), let start = AcademicParser.firstMonday(week: week, today: Date()),
                   let current = AcademicParser.current(available, start: start) {
                    selected = current.id
                }
                loading = false
                return
            }
            let value: AcademicReport
            switch service {
            case .grades:
                let page: String
                if term == "history" {
                    page = try await session.academicHTML("teach/grade/course/person!historyCourseGrade.action?projectType=MAJOR", form: nil)
                } else {
                    let shell = try await session.academicHTML("teach/grade/course/person.action?semester.id=\(term)", form: nil)
                    let resolved = AcademicParser.captures(#"semesterCalendar\s*\(\s*\{[^}]*?value\s*:\s*["'](\d+)"#, in: shell).first?[1] ?? term
                    page = try await session.academicHTML("teach/grade/course/person!search.action?semesterId=\(resolved)&projectType=&looked=yes", form: nil)
                }
                let doc = try await AcademicDocument.read(page)
                value = try AcademicParser.gradeReport(doc)
            case .exams:
                value = try await exams(term)
            case .plan, .catalog:
                let html = try await session.academicHTML(service == .plan ? "myPlanCompl.action" : "myPlan.action", form: nil)
                let doc = try await AcademicDocument.read(html)
                value = try service == .plan ? AcademicParser.plan(doc) : AcademicParser.catalog(doc)
            case .week:
                let html = try await session.academicHTML("homeExt!main.action", form: nil)
                guard let week = AcademicParser.week(html), let start = AcademicParser.firstMonday(week: week, today: Date()) else {
                    throw AcademicIssue("教务首页未提供当前教学周")
                }
                let format = DateFormatter()
                format.calendar = AcademicParser.schoolCalendar
                format.locale = Locale(identifier: "zh_CN")
                format.timeZone = AcademicParser.schoolCalendar.timeZone
                format.dateFormat = "yyyy年M月d日"
                value = AcademicReport(highlights: [AcademicField("当前教学周", "第 \(week) 周"), AcademicField("推算第一周周一", format.string(from: start))])
            }
            try Task.checkCancellation()
            report = value
        } catch {
            guard !Task.isCancelled else { return }
            if case CampusSessionError.academicLoginRequired = error {
                needsLogin = true; self.error = "教务登录已失效"
            } else { self.error = error.localizedDescription }
        }
        loading = false
    }

    @MainActor private func exams(_ term: String) async throws -> AcademicReport {
        let html = try await session.academicHTML("stdExamTable.action?semester.id=\(term)", form: nil)
        let shell = try await AcademicDocument.read(html)
        let batches = shell.batches.filter { $0 != "0" && Int($0) != nil }
        func query(_ batch: String) async throws -> [AcademicItem] {
            let html = try await session.academicHTML("stdExamTable!examTable.action?examBatch.id=\(batch)&semester.id=\(term)", form: nil)
            let doc = try await AcademicDocument.read(html)
            return try AcademicParser.exams(doc)
        }
        var results: [AcademicItem] = []
        do { results = try await query("0") }
        catch is AcademicIssue where !batches.isEmpty { /* The aggregate table is optional on EAMS. */ }
        if results.isEmpty {
            var visited = Set<String>(), seen = Set<[String]>()
            for batch in batches where visited.insert(batch).inserted {
                for var exam in try await query(batch) {
                    let key = [exam.title] + exam.fields.map(\.value)
                    if seen.insert(key).inserted { exam.id = results.count; results.append(exam) }
                }
            }
        }
        return AcademicReport(sections: [AcademicSection(id: 0, title: "考试安排", rows: results)])
    }
}
