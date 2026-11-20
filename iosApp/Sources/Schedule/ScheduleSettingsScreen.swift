import SwiftUI

func performScheduleEdit(_ error: Binding<String?>, _ action: () throws -> Void) {
    do { try action(); error.wrappedValue = nil }
    catch let failure { error.wrappedValue = failure.localizedDescription }
}

enum ScheduleColors {
    static let names = ["蓝色", "紫色", "绿色", "橙色", "粉色", "青色", "靛蓝", "棕色"]
    static let colors: [Color] = [.blue, .purple, .green, .orange, .pink, .teal, .indigo, .brown]
    static func tint(_ course: TimetableCourse) -> Color {
        let index = course.color ?? course.name.utf8.reduce(0) { ($0 * 31 + Int($1)) % colors.count }
        return colors[((index % colors.count) + colors.count) % colors.count]
    }
}

struct ScheduleSettingsScreen: View {
    @ObservedObject var model: ScheduleModel
    let owner: String
    @State private var error: String?
    var body: some View {
        List {
            Section {
                NavigationLink("课表管理") { TimetableManager(model: model, owner: owner) }
                if let table = model.archive.current, let key = model.archive.selected {
                    NavigationLink("课程管理") { CourseManager(model: model, owner: owner, key: key) }
                    NavigationLink("学期与开学日期") { TermEditor(model: model, owner: owner, key: key, table: table) }
                    NavigationLink("节次与作息") { TimesEditor(model: model, owner: owner, key: key, table: table) }
                    NavigationLink("假期与调休") { DayAdjustmentList(model: model, owner: owner, key: key) }
                }
            }
            if let table = model.archive.current, let key = model.archive.selected {
                Section("课表显示") {
                    displayToggle("课程序号", \.showNumber, table, key)
                    displayToggle("教师", \.showTeacher, table, key)
                    displayToggle("地点", \.showLocation, table, key)
                }
            }
            Section {
                NavigationLink("课程提醒") { CourseReminderSettings() }
                NavigationLink("导入、导出与系统日历") { CourseTransferScreen() }
            }
            if let error { Text(error).foregroundStyle(.red) }
        }.navigationTitle("课表设置")
    }
    private func displayToggle(_ title: String, _ option: WritableKeyPath<TimetableDisplay, Bool>,
                               _ table: CampusTimetable, _ key: String) -> some View {
        Toggle(title, isOn: Binding(get: { table.displayOptions[keyPath: option] }, set: { value in
            performScheduleEdit($error) {
                try model.changeTable(owner: owner, key: key) { saved in
                    var display = saved.displayOptions; display[keyPath: option] = value; saved.display = display
                }
            }
        }))
    }
}

private struct TimetableManager: View {
    @ObservedObject var model: ScheduleModel
    let owner: String
    @State private var deleting: String?
    @State private var renaming: String?
    @State private var error: String?
    var body: some View {
        List {
            ForEach(model.archive.sortedKeys, id: \.self) { key in
                if let table = model.archive.semesters[key] {
                    HStack {
                        Button { model.select(key) } label: {
                            HStack {
                                Image(systemName: model.archive.selected == key ? "checkmark.circle.fill" : "circle")
                                Text(table.title).multilineTextAlignment(.leading)
                            }.frame(maxWidth: .infinity, alignment: .leading)
                        }.buttonStyle(.borderless)
                        Menu {
                            Button("重命名") { renaming = key }
                            Button("删除课表", role: .destructive) { deleting = key }
                        } label: { Image(systemName: "ellipsis.circle").frame(width: 44, height: 44) }
                            .accessibilityLabel("\(table.title)的操作")
                    }
                }
            }
            NavigationLink("新建课表") { TableNameForm(model: model, owner: owner) }
            if let error = error ?? model.problem { Text(error).foregroundStyle(.red) }
        }.navigationTitle("课表管理")
         .navigationDestination(isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
             if let key = renaming, let table = model.archive.semesters[key] {
                 TableNameForm(model: model, owner: owner, key: key, name: table.title)
             }
         }
         .confirmationDialog("删除「\(deleting.flatMap { model.archive.semesters[$0]?.title } ?? "")」及其中的课程？",
                             isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }),
                             titleVisibility: .visible, presenting: deleting) { key in
             Button("删除课表", role: .destructive) {
                 performScheduleEdit($error) { try model.change(owner: owner) { $0.delete(key) } }
             }
             Button("取消", role: .cancel) {}
         }
    }
}

struct TableNameForm: View {
    @ObservedObject var model: ScheduleModel
    let owner: String
    let key: String?
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var date = SchoolCalendar.monday(of: Date())
    @State private var weeks = 20
    @State private var error: String?
    init(model: ScheduleModel, owner: String, key: String? = nil, name: String = "") {
        self.model = model; self.owner = owner; self.key = key; _name = State(initialValue: name)
    }
    var body: some View {
        Form {
            TextField("课表名称", text: $name)
            if key == nil {
                DatePicker("开学日期", selection: $date, displayedComponents: .date)
                Stepper("共 \(weeks) 周", value: $weeks, in: 1...53)
            }
            if let error { Text(error).foregroundStyle(.red) }
        }.navigationTitle(key == nil ? "新建课表" : "重命名")
         .toolbar { ToolbarItem(placement: .confirmationAction) {
             Button("保存") {
                 performScheduleEdit($error) {
                     try model.change(owner: owner) { archive in
                         if let key {
                             guard archive.semesters[key] != nil else { throw ScheduleIssue.message("课表已删除") }
                             let title = try archive.checkName(name, except: key); archive.semesters[key]?.displayName = title
                         } else {
                             try archive.create(name: name, dates: SchoolTermDates(semesterStartDate: SchoolCalendar.iso(date),
                                                                                 semesterTotalWeeks: weeks))
                         }
                     }; dismiss()
                 }
             }.disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
         }}
    }
}

private struct TermEditor: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    @Environment(\.dismiss) private var dismiss
    @State private var start: Date
    @State private var weeks: Int
    @State private var firstDay: Int
    @State private var error: String?
    init(model: ScheduleModel, owner: String, key: String, table: CampusTimetable) {
        self.model = model; self.owner = owner; self.key = key
        _start = State(initialValue: SchoolCalendar.date(table.dates.semesterStartDate) ?? Date())
        _weeks = State(initialValue: table.dates.semesterTotalWeeks)
        _firstDay = State(initialValue: table.dates.firstDayOfWeek ?? 1)
    }
    var body: some View {
        Form {
            DatePicker("开学日期", selection: $start, displayedComponents: .date)
            Stepper("共 \(weeks) 周", value: $weeks, in: 1...53)
            Picker("每周起始日", selection: $firstDay) {
                ForEach(1...7, id: \.self) { d in Text(SchoolCalendar.dayNames[d - 1]).tag(d) }
            }
            if let error { Text(error).foregroundStyle(.red) }
        }.navigationTitle("学期与开学日期").toolbar { ToolbarItem(placement: .confirmationAction) {
            Button("保存") {
                performScheduleEdit($error) {
                    try model.changeTable(owner: owner, key: key) { value in
                        guard value.courses.allSatisfy({ ($0.weeks.max() ?? 0) <= weeks })
                        else { throw ScheduleIssue.message("总周数不能少于最后一次上课的周次") }
                        value.dates.semesterStartDate = SchoolCalendar.iso(start)
                        value.dates.semesterTotalWeeks = weeks; value.dates.firstDayOfWeek = firstDay
                    }; dismiss()
                }
            }
        }}
    }
}

struct CourseManager: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    var body: some View {
        List {
            if let table = model.archive.semesters[key] {
                Section {
                    NavigationLink("新增课程") { CourseEditor(model: model, owner: owner, key: key, table: table, index: nil) }
                    if !table.courses.isEmpty {
                        NavigationLink("批量调课") { BulkCourseEditor(model: model, owner: owner, key: key, table: table) }
                    }
                }
                ForEach(1...7, id: \.self) { day in
                    let rows = table.courses.indices.filter { table.courses[$0].day == day }
                        .sorted { table.courses[$0].startSection < table.courses[$1].startSection }
                    if !rows.isEmpty {
                        Section(SchoolCalendar.dayNames[day - 1]) {
                            ForEach(rows, id: \.self) { i in
                                NavigationLink {
                                    CourseEditor(model: model, owner: owner, key: key, table: table, index: i)
                                } label: {
                                    VStack(alignment: .leading) {
                                        Text(table.courses[i].name)
                                        Text(table.courses[i].sectionText + " · " + table.courses[i].position).font(.caption)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.navigationTitle("课程管理")
    }
}
