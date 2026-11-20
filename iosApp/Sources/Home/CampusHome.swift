import SwiftUI

extension Notification.Name {
    static let xatuTimetableChanged = Notification.Name("XatuTimetableChanged")
}

struct CampusHome: View {
    @EnvironmentObject private var session: CampusSession
    @Environment(\.scenePhase) private var phase
    @AppStorage("xatu.appearance.style") private var style = "system"
    @State private var timetable: CampusTimetable?
    @State private var power = CampusPowerHistory()
    @State private var exam: AcademicUpcomingExam?
    @State private var courseDetail: CourseDetail?
    @State private var problem: String?
    var showCourses: () -> Void

    private func reload() {
        timetable = nil; power = CampusPowerHistory(); exam = nil; problem = nil
        do { timetable = try TimetableStore().load(account:session.timetableOwner).current }
        catch { problem = error.localizedDescription }
        guard !session.accountID.isEmpty else { return }
        do { power = try CampusPowerHistory.load(account:session.accountID)
             exam = try AcademicCache.nextExam(account:session.accountID) }
        catch { problem = error.localizedDescription }
    }

    var body: some View {
        TimelineView(.periodic(from:.now,by:60)) { tick in
            let now = tick.date
            List {
                Section {
                    LabeledContent(SchoolCalendar.iso(now)) {
                        if let week = timetable?.week(on:now) { Text("第 \(week) 教学周").bold() }
                    }
                    if let tbl = timetable {
                        let next = nextClass(tbl, now:now)
                        if let c = next {
                            Button { courseDetail = CourseDetail(course:c, table:tbl, date:now) } label: {
                                VStack(alignment:.leading, spacing:6) {
                                    Text(c.title).font(style == "compact" ? .headline : .title3.bold())
                                    Text([tbl.time(for:c,on:now), c.position.isEmpty ? nil : c.position].compactMap { $0 }.joined(separator:" · "))
                                        .font(.subheadline).foregroundStyle(.secondary)
                                }.padding(.vertical,style == "compact" ? 1 : 4)
                            }.buttonStyle(.plain)
                        } else {
                            Button(todayLabel(tbl,now:now), action:showCourses)
                        }
                    } else { Button("打开课表",action:showCourses) }
                } header: { Text("今日课程") }
                Section {
                    NavigationLink { AcademicExamsScreen() } label: {
                        Label {
                            VStack(alignment:.leading,spacing:3) {
                                Text(exam?.title ?? "考试安排")
                                if let exam { Text([exam.start.formatted(date:.abbreviated,time:.shortened),exam.room].filter{ !$0.isEmpty }.joined(separator:" · ")).font(.subheadline).foregroundStyle(.secondary) }
                            }
                        } icon: { Image(systemName:"calendar.badge.clock") }
                    }
                    NavigationLink { ElectricityScreen() } label: {
                        Label {
                            VStack(alignment:.leading,spacing:3) {
                                if let room = power.selected, let reading = power.readings.last {
                                    Text("宿舍电量  \(reading.formatted) 度").monospacedDigit()
                                    Text("\(room.address) · 上次更新 \(reading.time.formatted(date:.abbreviated,time:.shortened))")
                                        .font(.footnote).foregroundStyle(.secondary)
                                } else { Text("宿舍电费") }
                            }
                        } icon: { Image(systemName:"bolt") }
                    }
                }
                if let problem { Text(problem).foregroundStyle(.red) }
            }
            .environment(\.timeZone,SchoolCalendar.calendar.timeZone)
        }
        .navigationTitle("首页")
        .sheet(item:$courseDetail) { CourseDetailSheet(item:$0) }
        .onAppear { reload() }
        .onChange(of:session.timetableOwner) { _ in courseDetail = nil; reload() }
        .onChange(of:phase) { if $0 == .active { reload() } }
        .onReceive(NotificationCenter.default.publisher(for:.xatuTimetableChanged)) { _ in reload() }
        .onReceive(NotificationCenter.default.publisher(for:.academicCacheChanged)) { _ in reload() }
        .onReceive(NotificationCenter.default.publisher(for:.powerCacheChanged)) { _ in reload() }
    }
    private func todayLabel(_ table: CampusTimetable, now: Date) -> String {
        if table.week(on:now) == nil { return "查看学期课表" }
        return table.courses(on:now).isEmpty ? "今天没有课程" : "查看今日课表"
    }
    private func nextClass(_ table: CampusTimetable, now: Date) -> TimetableCourse? {
        let day = SchoolCalendar.calendar.startOfDay(for:now)
        let minute = Int(now.timeIntervalSince(day)) / 60
        return table.courses(on:now).compactMap { c -> (Int,TimetableCourse)? in
            guard let times = table.time(for:c,on:now)?.components(separatedBy:"–"), times.count == 2,
                  let start = CourseEvents.minutes(times[0]), let end = CourseEvents.minutes(times[1]), end > minute else { return nil }
            return (start,c)
        }.sorted { $0.0 < $1.0 }.first?.1
    }
}
