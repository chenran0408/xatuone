import SwiftUI

struct CourseEditor: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String, table: CampusTimetable
    let index: Int?
    @State private var original: TimetableCourse?
    let afterSave: (() -> Void)?
    @Environment(\.dismiss) private var dismiss
    @State private var course: TimetableCourse
    @State private var title: String
    @State private var number: String
    @State private var weeksText: String
    @State private var creditText: String
    @State private var deleting = false
    @State private var problem: String?

    init(model: ScheduleModel, owner: String, key: String, table: CampusTimetable, index: Int?, afterSave: (() -> Void)? = nil) {
        self.model = model; self.owner = owner; self.key = key; self.table = table
        self.index = index; self.afterSave = afterSave
        let old = index.flatMap { table.courses.indices.contains($0) ? table.courses[$0] : nil }
        _original = State(initialValue: old)
        let value = old ?? TimetableCourse(name: "", teacher: "", position: "", day: 1, startSection: 1,
                                           endSection: 1, weeks: [1])
        _course = State(initialValue: value)
        _title = State(initialValue: value.title); _number = State(initialValue: value.number ?? "")
        _weeksText = State(initialValue: old == nil ? "1-\(table.dates.semesterTotalWeeks)" : value.weeks.map(String.init).joined(separator: ","))
        _creditText = State(initialValue: value.credit.map { String($0) } ?? "")
    }

    var body: some View {
        Form {
            Section("课程") {
                TextField("课程名称", text: $title)
                TextField("课程序号", text: $number)
                TextField("教师", text: $course.teacher); TextField("上课地点", text: $course.position)
                TextField("学分", text: $creditText).keyboardType(.decimalPad)
                TextField("备注", text: Binding(get: { course.remark ?? "" }, set: { course.remark = $0 }))
                Picker("课程颜色", selection: Binding(get: { course.color ?? -1 }, set: { course.color = $0 < 0 ? nil : $0 })) {
                    Text("自动").tag(-1)
                    ForEach(ScheduleColors.names.indices, id: \.self) { i in Text(ScheduleColors.names[i]).tag(i) }
                }
            }
            Section("上课时间") {
                Picker("星期", selection: $course.day) {
                    ForEach(1...7, id: \.self) { d in Text(SchoolCalendar.dayNames[d - 1]).tag(d) }
                }
                TextField("周次，例如 1-4,6", text: $weeksText)
                Picker("开始节次", selection: $course.startSection) { slotNumbers }
                Picker("结束节次", selection: $course.endSection) { slotNumbers }
                Toggle("自定义时间", isOn: Binding(get: { course.isCustomTime == true }, set: { course.isCustomTime = $0 }))
                if course.isCustomTime == true {
                    TextField("开始时间 HH:mm", text: Binding(get: { course.customStartTime ?? "" }, set: { course.customStartTime = $0 }))
                    TextField("结束时间 HH:mm", text: Binding(get: { course.customEndTime ?? "" }, set: { course.customEndTime = $0 }))
                }
            }
            if let problem { Text(problem).foregroundStyle(.red) }
            if original != nil { Button("删除课程", role: .destructive) { deleting = true } }
        }
        .navigationTitle(original == nil ? "新增课程" : "编辑课程")
        .onChange(of: course.startSection) { start in course.endSection = max(start, course.endSection) }
        .toolbar { ToolbarItem(placement: .confirmationAction) { Button("保存") { write(deleting: false) } } }
        .confirmationDialog("删除这条课程安排及其全部周次？", isPresented: $deleting, titleVisibility: .visible) {
            Button("删除课程", role: .destructive) { write(deleting: true) }; Button("取消", role: .cancel) {}
        }
    }
    private var slotNumbers: some View {
        ForEach(table.slots, id: \.number) { s in Text("第 \(s.number) 节").tag(s.number) }
    }
    private func write(deleting: Bool) {
        performScheduleEdit($problem) {
            try model.changeTable(owner: owner, key: key) { live in
                if let index, !(live.courses.indices.contains(index) && live.courses[index] == original) {
                    throw ScheduleIssue.message("课程已变更，请重新打开")
                }
                if deleting, let index { live.courses.remove(at: index); return }
                var value = course
                let name = title.trimmingCharacters(in: .whitespacesAndNewlines)
                let code = number.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else { throw ScheduleIssue.message("请输入课程名称") }
                value.name = code.isEmpty ? name : name + " | " + code
                value.weeks = try TimetableCourse.parseWeeks(weeksText, limit: live.dates.semesterTotalWeeks)
                guard value.startSection <= value.endSection,
                      live.slots.contains(where: { $0.number == value.startSection }),
                      live.slots.contains(where: { $0.number == value.endSection }) else {
                    throw ScheduleIssue.message("请选择有效的开始和结束节次")
                }
                if value.isCustomTime == true {
                    guard let a = TeachingSlot.minutes(value.customStartTime ?? ""),
                          let b = TeachingSlot.minutes(value.customEndTime ?? ""), a < b
                    else { throw ScheduleIssue.message("自定义时间需使用 HH:mm，且结束晚于开始") }
                } else { value.customStartTime = nil; value.customEndTime = nil }
                let credit = creditText.trimmingCharacters(in: .whitespaces)
                if credit.isEmpty { value.credit = nil }
                else if let parsed = Double(credit), parsed.isFinite, parsed >= 0 { value.credit = parsed }
                else { throw ScheduleIssue.message("学分须为非负数") }
                if let index { live.courses[index] = value } else { live.courses.append(value) }
            }
            if let afterSave { afterSave() } else { dismiss() }
        }
    }
}

struct BulkCourseEditor: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    @State private var courses: [TimetableCourse]
    @Environment(\.dismiss) private var dismiss
    @State private var selected: Set<Int> = []
    @State private var offset = 0
    @State private var newDay = 0
    @State private var problem: String?
    init(model: ScheduleModel, owner: String, key: String, table: CampusTimetable) {
        self.model = model; self.owner = owner; self.key = key
        _courses = State(initialValue: table.courses)
    }
    var body: some View {
        Form {
            Section("调整方式") {
                Stepper("平移 \(offset) 周", value: $offset, in: -52...52)
                Picker("改到星期", selection: $newDay) {
                    Text("保持原星期").tag(0)
                    ForEach(1...7, id: \.self) { d in Text(SchoolCalendar.dayNames[d - 1]).tag(d) }
                }
            }
            Section("选择课程") {
                Button(selected.count == courses.count ? "取消全选" : "全选") {
                    selected = selected.count == courses.count ? [] : Set(courses.indices)
                }
                ForEach(courses.indices, id: \.self) { i in
                    let c = courses[i]
                    Button {
                        if selected.contains(i) { selected.remove(i) } else { selected.insert(i) }
                    } label: {
                        HStack {
                            Image(systemName: selected.contains(i) ? "checkmark.circle.fill" : "circle")
                            VStack(alignment: .leading) {
                                Text(c.name)
                                Text("\(SchoolCalendar.dayNames[c.day - 1]) · \(c.sectionText) · 第\(c.weeks.map(String.init).joined(separator: "、"))周")
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }.buttonStyle(.plain)
                }
            }
            if let problem { Text(problem).foregroundStyle(.red) }
        }.navigationTitle("批量调课").toolbar { ToolbarItem(placement: .confirmationAction) {
            Button("保存") {
                performScheduleEdit($problem) {
                    try model.changeTable(owner: owner, key: key) { value in
                        guard value.courses == courses else { throw ScheduleIssue.message("课表已变更，请重新选择") }
                        try value.adjustCourses(selected, movingWeeksBy: offset, toDay: newDay == 0 ? nil : newDay)
                    }; dismiss()
                }
            }.disabled(selected.isEmpty || (offset == 0 && newDay == 0))
        }}
    }
}
