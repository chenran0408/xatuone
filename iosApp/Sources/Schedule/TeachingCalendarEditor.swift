import SwiftUI

struct TimesEditor: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    @Environment(\.dismiss) private var dismiss
    @State private var base: [TeachingSlot]
    @State private var rules: [TimetableScheduleRule]
    @State private var title: String
    @State private var problem: String?
    init(model: ScheduleModel, owner: String, key: String, table: CampusTimetable) {
        self.model = model; self.owner = owner; self.key = key
        _base = State(initialValue: table.slots)
        _rules = State(initialValue: table.comboSchedule?.publicSchedules ?? [])
        _title = State(initialValue: table.comboSchedule?.name ?? "")
    }
    var body: some View {
        Form {
            Section("常规作息") { SlotRows(slots: $base) }
            Section("分日期作息") {
                if !rules.isEmpty { TextField("方案名称", text: $title) }
                ForEach(rules.indices, id: \.self) { i in
                    NavigationLink {
                        ScheduleRuleForm(rule: rules[i]) { rules[i] = $0 }
                    } label: {
                        VStack(alignment: .leading) { Text(rules[i].name); Text("\(rules[i].startDate) – \(rules[i].endDate)").font(.caption) }
                    }
                }.onDelete { rules.remove(atOffsets: $0) }
                NavigationLink("新增分日期作息") {
                    let start = model.archive.semesters[key]?.dates.semesterStartDate ?? SchoolCalendar.iso(Date())
                    ScheduleRuleForm(rule: TimetableScheduleRule(name: "", startDate: start, endDate: start, timeSlots: base)) {
                        rules.append($0)
                    }
                }
            }
            if let problem { Text(problem).foregroundStyle(.red) }
        }.navigationTitle("节次与作息").toolbar { ToolbarItem(placement: .confirmationAction) {
            Button("保存") {
                performScheduleEdit($problem) {
                    guard TeachingSlot.valid(base) else { throw ScheduleIssue.message("请检查节次时间，时段不能重叠") }
                    try model.changeTable(owner: owner, key: key) { table in
                        guard table.courses.allSatisfy({ $0.endSection <= base.count })
                        else { throw ScheduleIssue.message("仍有课程使用将被删除的节次") }
                        table.slots = base
                        table.comboSchedule = rules.isEmpty ? nil : TimetableComboSchedule(name: title, publicSchedules: rules)
                    }; dismiss()
                }
            }
        }}
    }
}

private struct SlotRows: View {
    @Binding var slots: [TeachingSlot]
    var body: some View {
        ForEach($slots, id: \.number) { $slot in
            VStack(alignment: .leading, spacing: 5) {
                Text("第 \(slot.number) 节").font(.subheadline.bold())
                HStack {
                    TextField("开始 HH:mm", text: $slot.startTime).accessibilityLabel("第\(slot.number)节开始时间")
                    Text("–").foregroundStyle(.secondary)
                    TextField("结束 HH:mm", text: $slot.endTime).accessibilityLabel("第\(slot.number)节结束时间")
                }.font(.body.monospacedDigit())
                TextField("节次别名", text: Binding(get: { slot.alias ?? "" }, set: { slot.alias = $0.isEmpty ? nil : $0 }))
                    .font(.subheadline)
            }
        }
        Button("新增节次") {
            slots.append(TeachingSlot(number: slots.count + 1, startTime: "", endTime: ""))
        }
        if slots.count > 1 { Button("移除最后一节", role: .destructive) { slots.removeLast() } }
    }
}

private struct ScheduleRuleForm: View {
    @State var rule: TimetableScheduleRule
    let apply: (TimetableScheduleRule) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var error: String?
    var body: some View {
        Form {
            TextField("作息名称", text: $rule.name)
            DatePicker("生效日期", selection: date(\.startDate), displayedComponents: .date)
            DatePicker("结束日期", selection: date(\.endDate), displayedComponents: .date)
            Section("节次") { SlotRows(slots: $rule.timeSlots) }
            if let error { Text(error).foregroundStyle(.red) }
        }.navigationTitle("分日期作息").toolbar { ToolbarItem(placement: .confirmationAction) {
            Button("确定") {
                guard !rule.name.trimmingCharacters(in: .whitespaces).isEmpty, rule.startDate <= rule.endDate,
                      TeachingSlot.valid(rule.timeSlots) else { error = "请检查名称、日期范围和节次时间"; return }
                apply(rule); dismiss()
            }
        }}
    }
    private func date(_ path: WritableKeyPath<TimetableScheduleRule, String>) -> Binding<Date> {
        Binding(get: { SchoolCalendar.date(rule[keyPath: path]) ?? Date() }, set: { rule[keyPath: path] = SchoolCalendar.iso($0) })
    }
}

struct DayAdjustmentList: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    @State private var problem: String?
    var body: some View {
        List {
            if let table = model.archive.semesters[key] {
                NavigationLink("添加假期或调休") { DayAdjustmentForm(model: model, owner: owner, key: key, table: table) }
                let dates = (table.adjustments ?? [:]).keys.sorted()
                ForEach(dates, id: \.self) { day in
                    if let value = table.adjustments?[day] {
                        VStack(alignment: .leading) {
                            Text(day + " · " + value.label)
                            Text(value.lessonDate.map { "按 \($0) 的课程上课" } ?? "全天停课").font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                }.onDelete { indices in
                    performScheduleEdit($problem) {
                        try model.changeTable(owner: owner, key: key) { value in
                            for i in indices { value.adjustments?.removeValue(forKey: dates[i]) }
                        }
                    }
                }
            }
            if let problem { Text(problem).foregroundStyle(.red) }
        }.navigationTitle("假期与调休").toolbar { EditButton() }
    }
}

private struct DayAdjustmentForm: View {
    @ObservedObject var model: ScheduleModel
    let owner: String, key: String
    @Environment(\.dismiss) private var dismiss
    @State private var isMakeup = false
    @State private var start: Date
    @State private var end: Date
    @State private var source: Date
    @State private var label = ""
    @State private var problem: String?
    init(model: ScheduleModel, owner: String, key: String, table: CampusTimetable) {
        self.model = model; self.owner = owner; self.key = key
        let date = table.week(on: Date()) == nil ? SchoolCalendar.date(table.dates.semesterStartDate) ?? Date() : Date()
        _start = State(initialValue: date); _end = State(initialValue: date); _source = State(initialValue: date)
    }
    var body: some View {
        Form {
            Picker("安排", selection: $isMakeup) { Text("放假").tag(false); Text("调休补课").tag(true) }.pickerStyle(.segmented)
            TextField("名称", text: $label)
            DatePicker(isMakeup ? "补课日期" : "开始日期", selection: $start, displayedComponents: .date)
            if isMakeup {
                DatePicker("按哪天的课表上课", selection: $source, displayedComponents: .date)
            } else {
                DatePicker("结束日期", selection: $end, in: start..., displayedComponents: .date)
            }
            if let problem { Text(problem).foregroundStyle(.red) }
        }.navigationTitle("新增安排")
         .onChange(of: start) { d in if end < d { end = d } }
         .toolbar { ToolbarItem(placement: .confirmationAction) {
             Button("保存") {
                 performScheduleEdit($problem) {
                     try model.changeTable(owner: owner, key: key) { value in
                         let day = SchoolCalendar.calendar.startOfDay(for: start)
                         let last = isMakeup ? day : SchoolCalendar.calendar.startOfDay(for: end)
                         guard value.week(on: day) != nil, value.week(on: last) != nil,
                               !isMakeup || (value.week(on: source) != nil && SchoolCalendar.iso(source) != SchoolCalendar.iso(day))
                         else { throw ScheduleIssue.message("日期须在学期内，补课与原课程日期须不同") }
                         var changes = value.adjustments ?? [:]
                         var cursor = day
                         let name = label.trimmingCharacters(in: .whitespaces)
                         let adjustment = TimetableDayAdjustment(label: name.isEmpty ? (isMakeup ? "调休" : "放假") : name,
                                                                 lessonDate: isMakeup ? SchoolCalendar.iso(source) : nil)
                         while cursor <= last {
                             let date = SchoolCalendar.iso(cursor)
                             guard changes[date] == nil else { throw ScheduleIssue.message("\(date) 已有安排，请先移除") }
                             changes[date] = adjustment
                             cursor = SchoolCalendar.add(1, to: cursor)
                         }
                         value.adjustments = changes
                     }; dismiss()
                 }
             }
         }}
    }
}
