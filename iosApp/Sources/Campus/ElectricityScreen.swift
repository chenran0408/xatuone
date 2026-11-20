import Charts
import SwiftUI

private enum PowerStatus {
    case loading, ready, login, empty, failed(String)

    init(error: Error) {
        if let sessionError = error as? CampusSessionError, case .paymentLoginRequired = sessionError { self = .login }
        else if let reason = error as? PowerError, reason == .login { self = .login }
        else if let reason = error as? PowerError, reason == .noReading { self = .empty }
        else { self = .failed(error.localizedDescription) }
    }
}

struct ElectricityScreen: View {
    @EnvironmentObject private var session: CampusSession
    var body: some View {
        Group {
            if session.accountID.isEmpty {
                Button("登录教务账号") { session.openAcademic() }
            } else {
                ElectricityDetail(owner: session.accountID).id(session.accountID)
            }
        }
        .navigationTitle("宿舍电费")
    }
}

private struct ElectricityDetail: View {
    @EnvironmentObject private var session: CampusSession
    let owner: String
    @State private var cache = CampusPowerHistory()
    @State private var status = PowerStatus.loading
    @State private var revision = 0
    @State private var completed: Int?
    @State private var picking = false
    @State private var pickAfterLogin = false
    @State private var liveReading: Date?
    @State private var days = 7
    @State private var byDay = false

    var body: some View {
        List {
            Section {
                if let room = cache.selected {
                    Text(room.address).font(.headline)
                    if let reading = cache.readings.last {
                        HStack(alignment: .firstTextBaseline, spacing: 4) {
                            Text(reading.formatted).font(.largeTitle.bold()).monospacedDigit()
                            Text("度").foregroundStyle(.secondary)
                        }
                        Text("\(liveReading == reading.time ? "更新于" : "上次更新") \(reading.time.formatted(date: .abbreviated, time: .standard))")
                            .font(.footnote).foregroundStyle(.secondary)
                        if let change = cache.recentChange {
                            LabeledContent("较上次查询", value: (change > 0 ? "+" : "") + change.formatted() + " 度")
                        }
                    }
                }
                switch status {
                case .loading: ProgressView("正在查询")
                case .login:
                    Button("登录缴费平台") {
                        session.openPayment {
                            guard session.accountID == owner else { return }
                            pickAfterLogin = cache.selected == nil
                            reload()
                        }
                    }
                case .failed(let message):
                    Text(message).foregroundStyle(.red)
                    Button("重试") { reload() }
                case .empty:
                    Text("该房间暂无电量数据").foregroundStyle(.secondary)
                    Button("刷新电量") { reload() }
                case .ready:
                    if cache.selected != nil { Button("刷新电量") { reload() } }
                }
                if canChoose {
                    Button(cache.selected == nil ? "选择宿舍房间" : "切换房间") { picking = true }
                }
            }
            if !cache.readings.isEmpty {
                Section("剩余电量趋势（度）") {
                    Picker("时间范围", selection: $days) {
                        Text("近 7 天").tag(7)
                        Text("近 30 天").tag(30)
                    }.pickerStyle(.segmented)
                    let end = Date()
                    let start = end.addingTimeInterval(-Double(days) * 86_400)
                    let readings = cache.readings.filter { (start...end).contains($0.time) }
                    if readings.count > 1 {
                        Chart(readings) { reading in
                            LineMark(x: .value("查询时间", reading.time),
                                     y: .value("剩余电量（度）", NSDecimalNumber(decimal: reading.amount).doubleValue))
                                .symbol(.circle)
                        }
                        .chartXScale(domain: start...end)
                        .frame(height: 155)
                    } else {
                        Text(readings.isEmpty ? "此期间暂无记录" : "此期间仅有一次查询").foregroundStyle(.secondary)
                    }
                }
                Section("查询记录") {
                    Picker("记录方式", selection: $byDay) {
                        Text("按次查询").tag(false)
                        Text("按日汇总").tag(true)
                    }.pickerStyle(.segmented)
                    if byDay {
                        ForEach(cache.daily.reversed()) { day in
                            VStack(alignment: .leading, spacing: 4) {
                                LabeledContent(day.last.time.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted, timeZone: AcademicParser.schoolCalendar.timeZone)),
                                               value: "\(day.last.formatted) 度")
                                if let drop = day.decrease {
                                    Text("较前日末次余额减少 \(drop.formatted()) 度").font(.subheadline).foregroundStyle(.secondary)
                                }
                            }
                        }
                    } else {
                        ForEach(cache.readings.reversed()) { reading in
                            LabeledContent(reading.time.formatted(date: .abbreviated, time: .shortened),
                                           value: "\(reading.formatted) 度")
                        }
                    }
                }
            }
        }
        .navigationDestination(isPresented: $picking) {
            PowerRoomPicker(owner: owner) { room in
                guard session.accountID == owner else { throw CancellationError() }
                var updated = cache
                updated.selected = room
                try updated.save(account: owner)
                cache = updated
                picking = false
                reload()
            }
        }
        .task(id: revision) { await load(revision) }
    }

    private var canChoose: Bool {
        switch status {
        case .ready, .empty: return true
        case .failed: return cache.selected != nil
        case .login, .loading: return false
        }
    }

    private func reload() {
        status = .loading
        liveReading = nil
        revision += 1
    }

    @MainActor private func load(_ attempt: Int) async {
        guard completed != attempt else { return }
        status = .loading
        liveReading = nil
        do {
            cache = try CampusPowerHistory.load(account: owner)
            if let room = cache.selected {
                let response = try await session.paymentJSON(CampusPowerAPI.balance, form: room.form)
                let amount: Decimal
                do {
                    amount = try CampusPowerAPI.remaining(response)
                } catch PowerError.noReading {
                    // A guest can also receive HTTP 200 with an empty payload.
                    let identity = try await session.paymentJSON(CampusPowerAPI.user)
                    try CampusPowerAPI.confirmUser(identity)
                    throw PowerError.noReading
                }
                try Task.checkCancellation()
                guard attempt == revision, owner == session.accountID, room.ids == cache.selected?.ids else { return }
                let reading = PowerReading(time: Date(), amount: amount)
                var updated = cache
                updated.record(reading, for: room)
                try updated.save(account: owner)
                cache = updated
                liveReading = reading.time
            } else {
                let response = try await session.paymentJSON(CampusPowerAPI.user)
                try CampusPowerAPI.confirmUser(response)
            }
            guard !Task.isCancelled, attempt == revision, owner == session.accountID else { return }
            status = .ready
            if pickAfterLogin { pickAfterLogin = false; picking = true }
        } catch {
            guard !Task.isCancelled, attempt == revision, owner == session.accountID else { return }
            status = PowerStatus(error: error)
        }
        completed = attempt
    }
}

private struct PowerRoomPicker: View {
    @EnvironmentObject private var session: CampusSession
    let owner: String
    let save: (PowerRoom) throws -> Void
    private let labels = ["缴费项目", "校区", "楼栋", "房间"]
    @State private var selection: [PowerOption] = []
    @State private var options: [PowerOption] = []
    @State private var status = PowerStatus.loading
    @State private var attempt = 0
    private struct Query: Equatable { let ids: [String]; let attempt: Int }

    var body: some View {
        List {
            if !selection.isEmpty {
                Section {
                    ForEach(Array(selection.enumerated()), id: \.offset) { index, option in
                        Button {
                            options = []
                            status = .loading
                            selection = Array(selection.prefix(index))
                        } label: {
                            LabeledContent(labels[index], value: option.name)
                        }
                    }
                }
            }
            if selection.count == 4 {
                Section {
                    Button("保存房间并查询") {
                        do { try save(PowerRoom(project: selection[0], area: selection[1], building: selection[2], room: selection[3])) }
                        catch { status = .failed(error.localizedDescription) }
                    }
                    if case let .failed(message) = status { Text(message).foregroundStyle(.red) }
                }
            } else {
                Section("选择" + labels[selection.count]) {
                    switch status {
                    case .loading: ProgressView()
                    case .login:
                        Button("登录缴费平台") {
                            session.openPayment { if owner == session.accountID { attempt += 1 } }
                        }
                    case let .failed(message):
                        Text(message).foregroundStyle(.red)
                        Button("重试") { attempt += 1 }
                    case .empty: Text("暂无可选项目").foregroundStyle(.secondary)
                    case .ready:
                        ForEach(options) { option in
                            Button(option.name) {
                                options = []
                                status = .loading
                                selection.append(option)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("选择宿舍")
        .task(id: Query(ids: selection.map(\.id), attempt: attempt)) { await fetchOptions() }
    }

    @MainActor private func fetchOptions() async {
        let chosen = selection
        guard chosen.count < 4 else { return }
        status = .loading
        let request = CampusPowerAPI.optionRequest(chosen)
        do {
            let raw = try await session.paymentJSON(request.path, form: request.form)
            let result = try CampusPowerAPI.options(raw, level: chosen.count)
            guard !Task.isCancelled, chosen == selection, owner == session.accountID else { return }
            options = result
            status = result.isEmpty ? .empty : .ready
        } catch {
            guard !Task.isCancelled, chosen == selection, owner == session.accountID else { return }
            status = PowerStatus(error: error)
        }
    }
}
