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
    var key: String {
        switch self {
        case .grades: return "grades"
        case .exams: return "exams"
        case .plan: return "plan"
        case .catalog: return "catalog"
        case .week: return "week"
        }
    }
}

struct AcademicExamsScreen: View {
    let semesterID: String?
    @EnvironmentObject private var session: CampusSession
    init(semesterID: String? = nil) { self.semesterID = semesterID }
    var body: some View { AcademicServiceScreen(service: .exams, initialTerm: semesterID).id(session.accountID) }
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
            Button {
                session.open(CampusSession.academicBase.appendingPathComponent("homeExt.action"), title: "教务系统")
            } label: { Label("教务系统网页", systemImage: "globe") }
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
    @State private var query = ""
    @State private var displayedTerm: String?
    @State private var timestamp: Date?
    @State private var isCached = false

    init(service: AcademicService, initialTerm: String? = nil) {
        self.service = service
        _selected = State(initialValue: initialTerm ?? "")
    }

    private func filtered(_ groups: [AcademicSection]) -> [AcademicSection] {
        let text = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return groups }
        return groups.compactMap { group in
            if service == .plan && group.title.localizedCaseInsensitiveContains(text) { return group }
            var filtered = group
            filtered.rows = group.rows.filter { $0.matches(text) }
            return filtered.rows.isEmpty ? nil : filtered
        }
    }

    var body: some View {
        List {
            if service.usesTerm && (!terms.isEmpty || service == .grades) {
                Picker("学期", selection: $selected) {
                    Text("选择学期").tag("")
                    if service == .grades { Text("全部历史成绩").tag("history") }
                    ForEach(terms) { Text($0.title).tag($0.id) }
                }
                if let date = terms.first(where: { $0.id == selected })?.startDate {
                    LabeledContent("开学日期", value: date)
                }
            }
            if loading {
                ProgressView("正在查询")
            } else if let error {
                Text(error).foregroundStyle(.secondary)
                if needsLogin { Button("登录教务系统") { session.openAcademic() } }
                else { Button("重新查询") { revision += 1 } }
            }
            if let report, displayedTerm == selected {
                if let timestamp {
                    LabeledContent(isCached ? "上次查询" : "更新于", value: timestamp.formatted(date: .abbreviated, time: .shortened))
                        .foregroundStyle(.secondary)
                }
                if service == .grades || service == .plan || service == .catalog {
                    TextField("搜索课程、代码或学期", text: $query).textInputAutocapitalization(.never)
                }
                if !report.highlights.isEmpty {
                    Section("汇总") { AcademicFields(values: report.highlights) }
                }
                if report.sections.allSatisfy({ $0.rows.isEmpty }) && report.highlights.isEmpty && service != .plan {
                    Text(service == .exams ? "本学期暂无考试安排" : "暂无课程成绩").foregroundStyle(.secondary)
                }
                let groups = filtered(report.sections)
                if !query.isEmpty && groups.isEmpty { Text("没有匹配的课程").foregroundStyle(.secondary) }
                ForEach(groups) { group in
                    if !group.rows.isEmpty || service == .plan {
                        Section {
                            if group.rows.isEmpty { Text(group.title) }
                            ForEach(group.rows) { row in
                                if row.fields.isEmpty {
                                    Text(row.title)
                                } else {
                                    NavigationLink {
                                        if service == .plan { PlanCourseDetail(course: row).id(session.accountID) }
                                        else { List { AcademicFields(values: row.fields) }.navigationTitle(row.title) }
                                    } label: {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(row.title)
                                            if service == .exams, let countdown = row.examCountdown(now: Date()) {
                                                Text(countdown).font(.subheadline).foregroundStyle(.tint)
                                            }
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
        .onChange(of: selected) { _ in query = "" }
        .onChange(of: session.browser?.id) { page in
            if page == nil && needsLogin { revision += 1 }
        }
        .task(id: AcademicQuery(term: selected, revision: revision)) { await load() }
    }

    @MainActor private func load() async {
        let term = selected
        let owner = session.accountID
        loading = true; error = nil; needsLogin = false; report = nil; displayedTerm = term; timestamp = nil; isCached = false
        var oldTerm: String?
        do {
            guard !owner.isEmpty else { throw CampusSessionError.academicLoginRequired }
            let local = try AcademicCache.load(account: owner)
            if service.usesTerm {
                terms = local.terms
                oldTerm = local.termSelection[service.key]
            }
            if let saved = local.reports[service.key]?[term] {
                report = saved.report; timestamp = saved.updatedAt; isCached = true
            }
            try await session.ensureAcademic()
            guard owner == session.accountID else { throw CancellationError() }
            if service.usesTerm && term.isEmpty {
                let html = try await session.academicHTML("dataQuery.action?tagId=&dataType=semesterCalendar", form: nil)
                let options = try await AcademicDocument.read(html).options
                let available = AcademicParser.semesters(html, options: options)
                guard !available.isEmpty else { throw AcademicIssue("教务系统未返回学期") }
                try Task.checkCancellation()
                guard owner == session.accountID, selected == term else { return }
                terms = available
                try AcademicCache.update(account: owner) { $0.terms = available }
                let home = try await session.academicHTML("homeExt!main.action", form: nil)
                try Task.checkCancellation()
                guard owner == session.accountID, selected == term else { return }
                // A user may pick a term while the home page is loading.
                if let week = AcademicParser.week(home), let start = AcademicParser.firstMonday(week: week, today: Date()),
                   let current = AcademicParser.current(available, start: start) {
                    selected = current.id
                } else {
                    error = "请手动选择学期"
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
            case .plan:
                let html = try await session.academicHTML("myPlanCompl.action", form: nil)
                let doc = try await AcademicDocument.read(html)
                value = try AcademicParser.plan(doc)
            case .catalog:
                value = try await loadAcademicCatalog(session)
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
            guard owner == session.accountID && selected == term else { return }
            let updated = Date()
            try AcademicCache.update(account: owner) {
                $0.reports[service.key, default: [:]][term] = AcademicSnapshot(report: value, updatedAt: updated)
                if service.usesTerm { $0.termSelection[service.key] = term }
            }
            report = value
            timestamp = updated; isCached = false
        } catch {
            guard !Task.isCancelled, owner == session.accountID, selected == term else { return }
            if term.isEmpty, service.usesTerm, let oldTerm, !oldTerm.isEmpty {
                selected = oldTerm
                return
            }
            if let sessionError = error as? CampusSessionError, case .academicLoginRequired = sessionError {
                needsLogin = true
                self.error = session.accountID.isEmpty ? "尚未登录教务系统" : "教务登录已失效"
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
        let sorted = results.sorted {
            let lhs = $0.examStart ?? .distantFuture, rhs = $1.examStart ?? .distantFuture
            return lhs == rhs ? $0.id < $1.id : lhs < rhs
        }
        return AcademicReport(sections: [AcademicSection(id: 0, title: "考试安排", rows: sorted)])
    }
}

@MainActor private func loadAcademicCatalog(_ session: CampusSession) async throws -> AcademicReport {
    let html = try await session.academicHTML("myPlan.action", form: nil)
    return try AcademicParser.catalog(await AcademicDocument.read(html))
}

private struct AcademicFields: View {
    let values: [AcademicField]
    var body: some View {
        ForEach(Array(values.enumerated()), id: \.offset) { _, field in
            LabeledContent(field.label) { Text(field.value).textSelection(.enabled) }
        }
    }
}

private struct PlanCourseDetail: View {
    let course: AcademicItem
    @EnvironmentObject private var session: CampusSession
    @State private var catalog: AcademicSnapshot?
    @State private var revision = 0
    @State private var loading = false
    @State private var error: String?
    @State private var needsLogin = false

    var body: some View {
        List {
            Section("学业完成情况") { AcademicFields(values: course.fields) }
            Section("培养方案") {
                if let catalog {
                    let info = AcademicParser.catalogMatch(for: course, in: catalog.report)
                    if let info { AcademicFields(values: info.fields.filter { $0.label != "课程代码" }) }
                    else { Text("培养方案中未找到这门课").foregroundStyle(.secondary) }
                    LabeledContent("上次更新", value: catalog.updatedAt.formatted(date: .abbreviated, time: .shortened))
                }
                if loading { ProgressView("正在读取培养方案") }
                if let error {
                    Text(error).foregroundStyle(.secondary)
                    if needsLogin { Button("登录教务系统") { session.openAcademic() } }
                    else { Button("重试") { revision += 1 } }
                }
            }
        }
        .navigationTitle(course.title)
        .onChange(of: session.browser?.id) { id in if id == nil && needsLogin { revision += 1 } }
        .task(id: revision) {
            let owner = session.accountID
            loading = true; error = nil; needsLogin = false
            do {
                if let existing = try AcademicCache.load(account: owner).reports["catalog"]?[""] {
                    catalog = existing
                } else {
                    try await session.ensureAcademic()
                    let result = try await loadAcademicCatalog(session)
                    try Task.checkCancellation()
                    guard owner == session.accountID else { return }
                    let snapshot = AcademicSnapshot(report: result, updatedAt: Date())
                    try AcademicCache.update(account: owner) { $0.reports["catalog", default: [:]][""] = snapshot }
                    catalog = snapshot
                }
            } catch {
                guard !Task.isCancelled, owner == session.accountID else { return }
                if let issue = error as? CampusSessionError, case .academicLoginRequired = issue { needsLogin = true }
                self.error = error.localizedDescription
            }
            loading = false
        }
    }
}
