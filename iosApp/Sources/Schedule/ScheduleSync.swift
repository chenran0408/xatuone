import SwiftUI
import WebKit

struct SemesterChoice: Identifiable {
    let id = UUID()
    let options: [String]
    let recommended: Int
}

@MainActor
private final class SemesterHandler: NSObject, WKScriptMessageHandlerWithReply {
    weak var webView: WKWebView?
    var choose: ((SemesterChoice, @escaping (Int?) -> Void) -> Void)?

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage,
                               replyHandler: @escaping @MainActor @Sendable (Any?, String?) -> Void) {
        let origin = message.frameInfo.securityOrigin
        guard let webView, message.webView === webView, message.frameInfo.isMainFrame,
              origin.protocol == "http", origin.host.lowercased() == "jwgl2018.xatu.edu.cn",
              origin.port == 0 || origin.port == 80,
              let payload = message.body as? [String: Any], let options = payload["options"] as? [String],
              !options.isEmpty, let choose else { replyHandler(nil, "无法读取教务学期"); return }
        choose(SemesterChoice(options: options, recommended: payload["recommended"] as? Int ?? -1)) { index in
            if let index, options.indices.contains(index) { replyHandler(index, nil) }
            else { replyHandler(NSNull(), nil) }
        }
    }
}

@MainActor
final class ScheduleModel: ObservableObject {
    @Published private(set) var archive = AccountTimetables()
    @Published private(set) var busy = false
    @Published var notice: String?
    @Published var problem: String?
    @Published private(set) var needsLogin = false
    @Published private(set) var choice: SemesterChoice?
    @Published private(set) var historical: TimetableDraft?
    private let store: TimetableStore
    private(set) var account = ""
    private var run: UUID?
    private var target: String?
    private var answer: ((Int?) -> Void)?

    init(store: TimetableStore = TimetableStore()) { self.store = store }

    func load(_ id: String) {
        cancel()
        account = id
        archive = AccountTimetables()
        notice = nil
        problem = nil
        needsLogin = false
        guard !id.isEmpty else { return }
        do { archive = try store.load(account: id) }
        catch { problem = "读取课表失败：\(error.localizedDescription)" }
    }

    func select(_ semester: String) {
        guard archive.semesters[semester] != nil, archive.selected != semester else { return }
        do { try change(owner: account) { next in
            guard next.semesters[semester] != nil else { throw ScheduleIssue.message("课表已删除") }
            next.selected = semester
        } }
        catch { problem = "保存学期失败：\(error.localizedDescription)" }
    }

    func reload(owner: String) {
        guard owner == account else { return }
        do { archive = try store.load(account: account) }
        catch { problem = "读取课表失败：\(error.localizedDescription)" }
    }

    func change(owner: String, _ edit: (inout AccountTimetables) throws -> Void) throws {
        guard owner == account else { throw ScheduleIssue.message("账号已切换，请重新打开课表") }
        var next = try store.load(account: owner)
        try edit(&next)
        try store.save(next, account: owner)
        archive = next
    }

    func changeTable(owner: String, key: String, _ edit: (inout CampusTimetable) throws -> Void) throws {
        try change(owner: owner) { next in
            guard var table = next.semesters[key] else { throw ScheduleIssue.message("课表已删除") }
            try edit(&table)
            next.semesters[key] = table
        }
    }

    func previous(for draft: TimetableDraft) -> CampusTimetable? {
        let key = target ?? archive.automaticTarget(for: draft.semester)
        return draft.matchingPrevious(key.flatMap { archive.semesters[$0] })
    }

    func reply(_ index: Int?) {
        let callback = answer
        answer = nil
        choice = nil
        callback?(index)
    }

    func cancel() {
        run = nil
        reply(nil)
        historical = nil
        busy = false
    }

    func confirm(date: Date, weeks: Int) {
        guard let draft = historical, weeks >= draft.lastWeek, weeks <= 53 else { return }
        do {
            try commit(draft, dates: SchoolTermDates(semesterStartDate: SchoolCalendar.iso(date), semesterTotalWeeks: weeks))
            historical = nil
            busy = false
            run = nil
        } catch { problem = "保存课表失败：\(error.localizedDescription)" }
    }

    private func commit(_ draft: TimetableDraft, dates: SchoolTermDates) throws {
        // Re-read before replacement. In particular, failure to decode existing data must never overwrite it.
        try change(owner: account) { next in try next.importCourses(draft, dates: dates, into: target) }
        notice = "已同步 \(draft.courses.count) 条课程安排"
        problem = nil
    }

    func sync(_ session: CampusSession, into target: String? = nil) async {
        guard !busy, !session.accountID.isEmpty, account == session.accountID else { return }
        self.target = target
        let token = UUID(), owner = account
        run = token
        busy = true
        notice = nil
        problem = nil
        needsLogin = false
        defer { if run == token, historical == nil { busy = false; run = nil } }
        do {
            try await session.ensureAcademic()
            guard run == token, session.accountID == owner else { return }
            guard let url = Bundle.main.url(forResource: "myschool", withExtension: "js") else {
                throw ScheduleIssue.message("缺少 XATU 课表适配器")
            }
            let source = try String(contentsOf: url, encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines)
            let invocation = "runImportFlow();"
            guard source.hasSuffix(invocation) else { throw ScheduleIssue.message("课表适配器入口已变更") }
            let handler = SemesterHandler()
            let view = session.webView
            handler.webView = view
            handler.choose = { [weak self, weak session] item, callback in
                guard let self, self.run == token, session?.accountID == owner else { callback(nil); return }
                self.answer = callback
                self.choice = item
            }
            let controller = view.configuration.userContentController
            let world = WKContentWorld.world(name: "XatuOne.Schedule." + token.uuidString)
            let channel = "xatuScheduleSemester"
            controller.addScriptMessageHandler(handler, contentWorld: world, name: channel)
            defer { controller.removeScriptMessageHandler(forName: channel, contentWorld: world) }
            let body = """
                if (location.origin !== 'http://jwgl2018.xatu.edu.cn') return {error: '请先登录教务账号'};
                const output = {courses: [], slots: [], config: null, semester: ''};
                let finished = false, cancelled = false, lastMessage = '';
                window.shiguangBridge = {
                  showToast(message) {lastMessage = String(message);},
                  notifyTaskCompletion() {finished = true;}
                };
                window.shiguangBridgePromise = {
                  async showSingleSelection(title, raw, recommended) {
                    const options = JSON.parse(raw);
                    const index = await window.webkit.messageHandlers.xatuScheduleSemester.postMessage({options, recommended});
                    if (index === null) {cancelled = true; return null;}
                    output.semester = options[index].replace(/（当前学期）$/, '');
                    return index;
                  },
                  async saveImportedCourses(raw) {output.courses = JSON.parse(raw); return true;},
                  async savePresetTimeSlots(raw) {output.slots = JSON.parse(raw); return true;},
                  async saveCourseConfig(raw) {output.config = JSON.parse(raw); return true;}
                };
                \(source.dropLast(invocation.count))
                await runImportFlow();
                if (cancelled) return null;
                if (!finished || !output.courses.length || !output.slots.length || !output.semester)
                    return {error: lastMessage || '同步未完成'};
                return {data: JSON.stringify(output)};
                """
            let result = try await view.callAsyncJavaScript(body, arguments: [:], in: nil, contentWorld: world)
            guard run == token, session.accountID == owner, let response = result as? [String: Any] else { return }
            if let message = response["error"] as? String { throw ScheduleIssue.message(message) }
            guard let string = response["data"] as? String else { throw ScheduleIssue.message("课表数据读取失败") }
            let draft = try JSONDecoder().decode(TimetableDraft.self, from: Data(string.utf8))
            if let dates = draft.usableDates(previous: previous(for: draft)) {
                try commit(draft, dates: dates)
            } else {
                historical = draft
            }
        } catch CampusSessionError.academicLoginRequired {
            if run == token, session.accountID == owner {
                problem = "教务登录已失效"
                needsLogin = true
            }
        } catch {
            if run == token, session.accountID == owner {
                problem = error.localizedDescription
            }
        }
    }
}
