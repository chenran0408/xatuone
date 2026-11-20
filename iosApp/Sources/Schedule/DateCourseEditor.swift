import SwiftUI

private struct DateMovePreview {
    let source: Date
    let target: Date
    let mode: DayCourseMove
    let sourceCourses: [TimetableCourse]
    let targetCourses: [TimetableCourse]

    init(source: Date, target: Date, mode: DayCourseMove, table: CampusTimetable) {
        self.source = source; self.target = target; self.mode = mode
        sourceCourses = table.regularCourses(on: source)
        targetCourses = table.regularCourses(on: target)
    }

    var hasChanges: Bool {
        !SchoolCalendar.calendar.isDate(source, inSameDayAs: target)
            && (!sourceCourses.isEmpty || (mode != .merge && !targetCourses.isEmpty))
    }
}

struct DateCourseEditor: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    @Environment(\.dismiss) private var dismiss
    @State private var source: Date
    @State private var target: Date
    @State private var mode = DayCourseMove.merge
    @State private var overwrite: DateMovePreview?
    @State private var problem: String?

    init(model: ScheduleModel, owner: String, key: String, table: CampusTimetable) {
        self.model = model; self.owner = owner; self.key = key
        let today = SchoolCalendar.calendar.startOfDay(for: Date())
        let first = table.week(on: today) == nil ? SchoolCalendar.date(table.dates.semesterStartDate) ?? today : today
        let next = SchoolCalendar.add(1, to: first)
        _source = State(initialValue: first)
        _target = State(initialValue: table.week(on: next) == nil ? first : next)
    }

    var body: some View {
        Form {
            if let table = model.archive.semesters[key], let start = SchoolCalendar.date(table.dates.semesterStartDate) {
                let last = SchoolCalendar.add(table.dates.semesterTotalWeeks * 7 - 1, to: table.startOfWeek(containing: start))
                let preview = DateMovePreview(source: source, target: target, mode: mode, table: table)
                Section {
                    Text(table.title)
                    Picker("调课方式", selection: $mode) {
                        ForEach(DayCourseMove.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                    }.pickerStyle(.segmented)
                }
                daySection("来源日", date: $source, range: start...last, table: table, courses: preview.sourceCourses)
                daySection("目标日", date: $target, range: start...last, table: table, courses: preview.targetCourses)
                Button(actionTitle) {
                    if mode == .overwrite && !preview.targetCourses.isEmpty { overwrite = preview }
                    else { save(preview) }
                }.disabled(!preview.hasChanges)
            } else {
                Text("无法读取课表和开学日期")
            }
            if let problem { Text(problem).foregroundStyle(.red) }
        }.navigationTitle("按日期调课")
         .confirmationDialog("替换目标日的 \(overwrite?.targetCourses.count ?? 0) 条课程安排？",
                             isPresented: Binding(get: { overwrite != nil }, set: { if !$0 { overwrite = nil } }),
                             titleVisibility: .visible, presenting: overwrite) { preview in
             Button("覆盖目标日", role: .destructive) { save(preview) }
             Button("取消", role: .cancel) {}
         }
    }

    private var actionTitle: String {
        switch mode {
        case .merge: return "移到目标日并合并"
        case .overwrite: return "覆盖目标日课程"
        case .exchange: return "交换两天课程"
        }
    }

    private func daySection(_ title: String, date: Binding<Date>, range: ClosedRange<Date>,
                            table: CampusTimetable, courses: [TimetableCourse]) -> some View {
        Section {
            DatePicker(title, selection: date, in: range, displayedComponents: .date)
            if let adjustment = table.adjustments?[SchoolCalendar.iso(date.wrappedValue)] {
                Text("\(adjustment.label) · 此处调整原排课").foregroundStyle(.secondary)
            }
            if courses.isEmpty { Text("无原排课程").foregroundStyle(.secondary) }
            ForEach(courses.indices, id: \.self) { index in
                let course = courses[index]
                VStack(alignment: .leading, spacing: 3) {
                    Text(course.name)
                    Text([course.sectionText, course.position].filter { !$0.isEmpty }.joined(separator: " · "))
                        .font(.subheadline).foregroundStyle(.secondary)
                }
            }
        }
    }

    private func save(_ preview: DateMovePreview) {
        performScheduleEdit($problem) {
            try model.changeTable(owner: owner, key: key) { table in
                guard table.regularCourses(on: preview.source) == preview.sourceCourses,
                      table.regularCourses(on: preview.target) == preview.targetCourses else {
                    throw ScheduleIssue.message("两天的排课已变更，请重新确认")
                }
                try table.moveCourses(from: preview.source, to: preview.target, mode: preview.mode)
            }
            model.notice = "已调整 \(SchoolCalendar.iso(preview.source)) 与 \(SchoolCalendar.iso(preview.target)) 的课程"
            dismiss()
        }
    }
}
