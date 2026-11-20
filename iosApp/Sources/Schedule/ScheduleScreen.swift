import SwiftUI

struct ScheduleScreen: View {
    @EnvironmentObject private var session: CampusSession
    @StateObject private var model = ScheduleModel()
    @State private var date = Date()
    @State private var weekly = false
    @State private var detail: CourseDetail?

    var body: some View {
        Group {
            if let choice = model.choice {
                List {
                    Section("选择同步学期") {
                        ForEach(choice.options.indices, id: \.self) { i in
                            Button { model.reply(i) } label: {
                                HStack { Text(choice.options[i]); Spacer()
                                    if i == choice.recommended { Image(systemName: "calendar.badge.clock") }
                                }
                            }
                        }
                    }
                    Button("取消", role: .cancel) { model.reply(nil) }
                }
            } else if let draft = model.historical {
                HistoricalTermForm(draft: draft, previous: model.archive.semesters[draft.semester],
                                   problem: model.problem, confirm: model.confirm, cancel: model.cancel)
            } else {
                VStack(spacing: 0) {
                    if let error = model.problem {
                        HStack {
                            Label(error, systemImage: "exclamationmark.circle"); Spacer()
                            if model.needsLogin { Button("重新登录") { session.openAcademic() } }
                        }
                            .font(.callout).foregroundStyle(.red).padding(.horizontal).padding(.vertical, 6)
                    } else if let notice = model.notice {
                        Label(notice, systemImage: "checkmark.circle").font(.callout).padding(.vertical, 4)
                    }
                    if let table = model.archive.current {
                        tableHeader(table)
                        if weekly {
                            WeekTimetable(table: table, date: date) { detail = CourseDetail(course: $0, table: table) }
                        } else {
                            List {
                                let courses = table.courses(on: date)
                                if table.week(on: date) == nil { Text("所选日期不在本学期").foregroundStyle(.secondary) }
                                else if courses.isEmpty { Text("当天没有课程").foregroundStyle(.secondary) }
                                ForEach(courses.indices, id: \.self) { i in
                                    let course = courses[i]
                                    Button { detail = CourseDetail(course: course, table: table) } label: {
                                        VStack(alignment: .leading, spacing: 5) {
                                            Text(course.title).font(.headline)
                                            Text([table.time(for: course), course.position].compactMap { $0 }.joined(separator: " · "))
                                            Text([course.sectionText, course.teacher].joined(separator: " · ")).foregroundStyle(.secondary)
                                        }.font(.subheadline).padding(.vertical, 3)
                                    }.buttonStyle(.plain)
                                }
                            }.listStyle(.insetGrouped)
                        }
                    } else {
                        VStack(spacing: 14) {
                            Image(systemName: "calendar").font(.largeTitle).foregroundStyle(.secondary)
                            Text(session.accountID.isEmpty ? "登录后查看课表" : "还没有同步课表")
                            if session.accountID.isEmpty {
                                Button("登录教务") { session.openAcademic() }.buttonStyle(.borderedProminent)
                            }
                        }.frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
        }
        .navigationTitle("课程表")
        .toolbar {
            if model.busy && model.choice == nil && model.historical == nil {
                ToolbarItem(placement: .primaryAction) { ProgressView().accessibilityLabel("正在同步课表") }
            } else if !model.busy && !session.accountID.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    Button("同步课表") { Task { await model.sync(session) } }
                }
            }
        }
        .sheet(item: $detail) { CourseDetailSheet(item: $0) }
        .onAppear { loadAccount() }
        .onChange(of: session.accountID) { _ in detail = nil; loadAccount() }
        .onChange(of: model.archive.selected) { _ in setInitialDay() }
        .environment(\.timeZone, SchoolCalendar.calendar.timeZone)
    }

    private func loadAccount() {
        guard model.account != session.accountID else { return }
        model.load(session.accountID)
        setInitialDay()
    }

    private func setInitialDay() {
        guard let table = model.archive.current, let start = SchoolCalendar.date(table.dates.semesterStartDate) else { return }
        date = table.week(on: Date()) == nil ? start : Date()
    }

    private func tableHeader(_ table: CampusTimetable) -> some View {
        VStack(spacing: 8) {
            HStack {
                Menu {
                    ForEach(model.archive.semesters.keys.sorted(by: >), id: \.self) { name in
                        Button { model.select(name) } label: {
                            if table.semester == name { Label(name, systemImage: "checkmark") } else { Text(name) }
                        }
                    }
                } label: {
                    HStack { Text(table.semester).multilineTextAlignment(.leading); Image(systemName: "chevron.down").font(.caption) }
                }
                Spacer()
            }
            Picker("视图", selection: $weekly) {
                Text("日课表").tag(false); Text("周课表").tag(true)
            }.pickerStyle(.segmented)
            if weekly {
                let week = table.pageWeek(containing: date)
                HStack {
                    Button {
                        if let week, let previous = table.date(inWeek: week - 1, keepingWeekdayOf: date) { date = previous }
                    } label: { Image(systemName: "chevron.left").frame(minWidth: 44, minHeight: 44) }
                        .accessibilityLabel("上一周").disabled((week ?? 0) <= 1)
                    Spacer()
                    Menu {
                        ForEach(1...max(1, table.dates.semesterTotalWeeks), id: \.self) { w in
                            Button("第 \(w) 周") {
                                if let selected = table.date(inWeek: w, keepingWeekdayOf: date) { date = selected }
                            }
                        }
                    } label: { Text(week.map { "第 \($0) 周" } ?? "选择教学周").font(.headline) }
                    Spacer()
                    Button {
                        if let week, let next = table.date(inWeek: week + 1, keepingWeekdayOf: date) { date = next }
                    } label: { Image(systemName: "chevron.right").frame(minWidth: 44, minHeight: 44) }
                        .accessibilityLabel("下一周")
                        .disabled(week.map { $0 >= table.dates.semesterTotalWeeks } ?? true)
                }
            } else {
                DatePicker(table.week(on: date).map { "第 \($0) 周" } ?? "日期",
                           selection: $date, displayedComponents: .date)
            }
        }.padding(.horizontal).padding(.vertical, 6)
    }
}

private struct WeekTimetable: View {
    let table: CampusTimetable
    let date: Date
    let select: (TimetableCourse) -> Void
    @ScaledMetric(relativeTo: .body) private var columnWidth: CGFloat = 77

    var body: some View {
        let monday = SchoolCalendar.monday(of: date)
        let dayCourses = table.coursesInWeek(containing: date)
        ScrollView([.horizontal, .vertical]) {
            Grid(horizontalSpacing: 4, verticalSpacing: 4) {
                GridRow {
                    Text("节次").font(.caption)
                    ForEach(0..<7) { day in
                        let shown = SchoolCalendar.add(day, to: monday)
                        VStack(spacing: 3) {
                            Text(SchoolCalendar.dayNames[day]).font(.subheadline.bold())
                            Text(String(SchoolCalendar.iso(shown).suffix(5))).font(.caption.monospacedDigit())
                        }.frame(width: columnWidth, alignment: .center)
                         .foregroundStyle(SchoolCalendar.calendar.isDateInToday(shown) ? Color.accentColor : Color.primary)
                    }
                }
                ForEach(table.slots, id: \.number) { slot in
                    GridRow(alignment: .top) {
                        VStack(spacing: 4) { Text("\(slot.number)").bold(); Text(slot.startTime); Text(slot.endTime) }
                            .font(.caption.monospacedDigit()).frame(minWidth: 45, minHeight: 46)
                            .accessibilityLabel("第 \(slot.number) 节，\(slot.startTime) 至 \(slot.endTime)")
                        ForEach(1...7, id: \.self) { day in
                            let items = dayCourses[day - 1].filter { ($0.startSection...$0.endSection).contains(slot.number) }
                            VStack(alignment: .leading, spacing: 4) {
                                ForEach(items.indices, id: \.self) { i in
                                    let course = items[i]
                                    Button { select(course) } label: {
                                        VStack(alignment: .leading, spacing: 3) {
                                            if course.startSection == slot.number {
                                                Text(course.title).font(.caption.bold())
                                                Text(course.position).font(.caption2).foregroundStyle(.secondary)
                                            } else {
                                                Image(systemName: "arrow.down").font(.caption)
                                            }
                                        }.padding(5).frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                                         .background(Color.accentColor.opacity(0.14), in: RoundedRectangle(cornerRadius: 6))
                                    }.buttonStyle(.plain)
                                     .accessibilityLabel("\(course.title)，\(course.sectionText)，\(course.position)")
                                }
                            }.frame(width: columnWidth).frame(minHeight: 46, alignment: .top)
                        }
                    }
                    Divider().gridCellUnsizedAxes(.horizontal)
                }
            }.padding(.horizontal, 10).padding(.vertical, 8)
        }
    }
}

private struct CourseDetail: Identifiable {
    let id = UUID()
    let course: TimetableCourse
    let table: CampusTimetable
}

private struct CourseDetailSheet: View {
    let item: CourseDetail
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            let course = item.course
            List {
                Text(course.title).font(.title3.bold())
                if let number = course.number { LabeledContent("课程序号", value: number) }
                LabeledContent("教师", value: course.teacher)
                LabeledContent("地点", value: course.position)
                LabeledContent("周次", value: course.weeks.map(String.init).joined(separator: "、"))
                if (1...7).contains(course.day) { LabeledContent("星期", value: SchoolCalendar.dayNames[course.day - 1]) }
                LabeledContent("节次", value: course.sectionText)
                if let time = item.table.time(for: course) { LabeledContent("时间", value: time) }
                if let credit = course.credit { LabeledContent("学分", value: credit.formatted()) }
            }.navigationTitle("课程详情").navigationBarTitleDisplayMode(.inline)
             .toolbar { ToolbarItem(placement: .confirmationAction) { Button("完成") { dismiss() } } }
        }
    }
}

private struct HistoricalTermForm: View {
    let draft: TimetableDraft
    let previous: CampusTimetable?
    let problem: String?
    let confirm: (Date, Int) -> Void
    let cancel: () -> Void
    @State private var start = ""
    @State private var weeks = ""
    @State private var picking = false
    @State private var candidate = Date()

    var body: some View {
        Form {
            Section(draft.semester) {
                Button { candidate = SchoolCalendar.date(start) ?? Date(); picking = true } label: {
                    LabeledContent("开学日期", value: start.isEmpty ? "选择日期" : start)
                }
                TextField("学期总周数", text: $weeks).keyboardType(.numberPad)
                Text("课程排至第 \(draft.lastWeek) 周").foregroundStyle(.secondary)
            }
            if let problem { Text(problem).foregroundStyle(.red) }
            Button("保存课表") { if let date = SchoolCalendar.date(start), let count = Int(weeks) { confirm(date, count) } }
                .disabled(SchoolCalendar.date(start) == nil || !(draft.lastWeek...max(53, draft.lastWeek)).contains(Int(weeks) ?? 0))
            Button("取消", role: .cancel, action: cancel)
        }
        .onAppear {
            if start.isEmpty { start = draft.config?.semesterStartDate ?? previous?.dates.semesterStartDate ?? "" }
            if weeks.isEmpty, let n = draft.config?.semesterTotalWeeks ?? previous?.dates.semesterTotalWeeks,
               n >= draft.lastWeek { weeks = String(n) }
        }
        .sheet(isPresented: $picking) {
            NavigationStack {
                DatePicker("开学日期", selection: $candidate, displayedComponents: .date).datePickerStyle(.graphical).padding()
                    .navigationTitle("开学日期").navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("取消") { picking = false } }
                        ToolbarItem(placement: .confirmationAction) { Button("确定") { start = SchoolCalendar.iso(candidate); picking = false } }
                    }
            }
        }
    }
}
