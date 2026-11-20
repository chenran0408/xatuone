import SwiftUI
import UniformTypeIdentifiers

struct CampusFile: FileDocument {
    static var readableContentTypes: [UTType] { [.data] }
    let bytes: Data
    init(_ data: Data) { bytes = data }
    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw CocoaError(.fileReadCorruptFile) }
        bytes = data
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents:bytes) }
}

struct CourseTransferScreen: View {
    @EnvironmentObject private var session: CampusSession
    @State private var table: CampusTimetable?
    @State private var tableKey: String?
    @State private var importing = false
    @State private var pending: Data?
    @State private var name = ""
    @State private var start = Date()
    @State private var weeks = 20
    @State private var importOwner = ""
    @State private var output: CampusFile?
    @State private var outputName = ""
    @State private var outputType = UTType.json
    @State private var exporting = false
    @State private var alarm = -1
    @State private var busy = false
    @State private var message: String?
    private let store = TimetableStore()

    var body: some View {
        Form {
            if pending != nil {
                Section("导入为新课表") {
                    TextField("课表名称", text:$name)
                    DatePicker("开学日期", selection:$start, displayedComponents:.date)
                    Stepper("共 \(weeks) 周", value:$weeks, in:1...53)
                    Button("保存新课表") { saveImport() }.disabled(name.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty)
                    Button("取消",role:.cancel) { pending = nil; message = nil }
                }
            } else {
                Section("课表文件") {
                    Button("导入课表文件") { importOwner = session.timetableOwner; importing = true }
                    if let table {
                        LabeledContent("当前课表",value:table.title)
                        Button("导出课表文件") { makeExport(table, calendar:false) }
                    }
                }
                if let table {
                    Section("系统日历") {
                        Picker("日历提醒", selection:$alarm) {
                            Text("关闭").tag(-1)
                            ForEach([0,5,10,15,30,60], id:\.self) { m in Text(m == 0 ? "上课时" : "提前 \(m) 分钟").tag(m) }
                        }
                        Button("导出 ICS 文件") { makeExport(table, calendar:true) }
                        Button("同步到系统日历") { sync(table) }.disabled(busy)
                        if busy { ProgressView() }
                    }
                }
            }
            if let message { Text(message) }
        }
        .navigationTitle("课表文件与系统日历")
        .environment(\.timeZone,SchoolCalendar.calendar.timeZone)
        .task(id:session.timetableOwner) { pending = nil; readCurrent() }
        .fileImporter(isPresented:$importing, allowedContentTypes:[.data]) { result in
            do {
                let url = try result.get()
                guard importOwner == session.timetableOwner else { return }
                let access = url.startAccessingSecurityScopedResource()
                defer { if access { url.stopAccessingSecurityScopedResource() } }
                let bytes = try Data(contentsOf:url)
                let json = try CourseExchange.object(bytes)
                guard json["courses"] is [[String:Any]] else { throw ScheduleIssue.message("未找到课表数据") }
                let config = json["config"] as? [String:Any]
                name = json["displayName"] as? String ?? json["semester"] as? String ?? url.deletingPathExtension().lastPathComponent
                start = (config?["semesterStartDate"] as? String).flatMap(SchoolCalendar.date) ?? Date()
                weeks = min(53,max(1,config?["semesterTotalWeeks"] as? Int ?? 20))
                pending = bytes; message = nil
            } catch { if (error as NSError).code != NSUserCancelledError { message = error.localizedDescription } }
        }
        .fileExporter(isPresented:$exporting,document:output,contentType:outputType,defaultFilename:outputName) { result in
            switch result { case .success: message = "文件已导出"
            case .failure(let error): if (error as NSError).code != NSUserCancelledError { message = error.localizedDescription } }
        }
    }
    private func readCurrent() {
        do {
            let archive = try store.load(account:session.timetableOwner)
            table = archive.current; tableKey = archive.selected
        } catch { table = nil; tableKey = nil; message = error.localizedDescription }
    }
    private func saveImport() {
        guard let pending, importOwner == session.timetableOwner else { return }
        do {
            var archive = try store.load(account:importOwner)
            let title = try archive.checkName(name)
            let imported = try CourseExchange.imported(pending, name:title,start:start,weeks:weeks,fallbackSlots:table?.slots ?? TeachingSlot.campusDefaults)
            let key = UUID().uuidString
            archive.semesters[key] = imported; archive.selected = key
            try store.save(archive, account:importOwner)
            self.pending = nil; table = imported; tableKey = key; message = "已导入 \(imported.courses.count) 条课程安排"
        } catch { message = error.localizedDescription }
    }
    private func makeExport(_ table: CampusTimetable, calendar: Bool) {
        do {
            output = try CampusFile(calendar ? CourseEvents.ics(CourseEvents.occurrences(table),alarm:alarm < 0 ? nil : alarm) : CourseExchange.export(table))
            outputName = table.title + (calendar ? ".ics" : ".json")
            outputType = calendar ? (UTType(filenameExtension:"ics") ?? .data) : .json
            message = nil; exporting = true
        } catch { message = error.localizedDescription }
    }
    private func sync(_ table: CampusTimetable) {
        guard !busy, let tableKey else { return }
        let owner = session.timetableOwner
        busy = true; message = nil
        Task { @MainActor in
            defer { busy = false }
            do {
                let count = try await CourseCalendar().sync(table,owner:owner,tableKey:tableKey,alarm:alarm < 0 ? nil : alarm) { owner == session.timetableOwner }
                message = "已同步 \(count) 条日历安排"
            } catch { message = error.localizedDescription }
        }
    }
}
