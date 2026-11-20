import Foundation
import CoreFoundation

struct PowerOption: Codable, Hashable, Identifiable {
    let id: String
    let name: String
}

struct PowerRoom: Codable, Hashable {
    let project: PowerOption
    let area: PowerOption
    let building: PowerOption
    let room: PowerOption

    var ids: [String] { [project.id, area.id, building.id, room.id] }
    var address: String { [area.name, building.name, room.name].joined(separator: " · ") }
    var form: [String: String] {
        ["projectId": project.id, "areaid": area.id, "buildid": building.id, "roomid": room.id]
    }
}

struct PowerReading: Codable, Identifiable {
    let time: Date
    let amount: Decimal
    var id: Date { time }
    var formatted: String {
        amount.formatted(.number.grouping(.never).precision(.fractionLength(2)))
    }
}

struct CampusPowerHistory: Codable {
    private struct RoomHistory: Codable {
        let ids: [String]
        var readings: [PowerReading]
    }
    var selected: PowerRoom?
    private var rooms: [RoomHistory] = []

    init() {}
    var readings: [PowerReading] {
        rooms.first { $0.ids == selected?.ids }?.readings ?? []
    }

    mutating func record(_ reading: PowerReading, for room: PowerRoom) {
        if let index = rooms.firstIndex(where: { $0.ids == room.ids }) {
            var history = rooms[index].readings.filter { $0.time != reading.time }
            history.append(reading)
            rooms[index].readings = Array(history.sorted { $0.time < $1.time }.suffix(400))
        } else {
            rooms.append(RoomHistory(ids: room.ids, readings: [reading]))
        }
    }

    static func load(account: String, defaults: UserDefaults = .standard) throws -> Self {
        guard !account.isEmpty, let data = defaults.data(forKey: "xatu.power.v1." + account) else { return Self() }
        return try JSONDecoder().decode(Self.self, from: data)
    }

    func save(account: String, defaults: UserDefaults = .standard) throws {
        guard !account.isEmpty else { throw PowerError.message("请先登录教务账号") }
        defaults.set(try JSONEncoder().encode(self), forKey: "xatu.power.v1." + account)
    }
}

enum PowerError: LocalizedError, Equatable {
    case login, noReading, invalidData, message(String)
    var errorDescription: String? {
        switch self {
        case .login: return "请登录校园缴费平台"
        case .noReading: return "缴费平台暂未返回该房间的电量"
        case .invalidData: return "缴费平台返回的数据格式有误"
        case let .message(message): return message
        }
    }
}

enum CampusPowerAPI {
    static let user = "pay/queryUserInfo"
    static let projects = "pay/project/getAllProjectList"
    static let controller = "pay/web/payEleCostController/"
    static let balance = controller + "querySydl"

    private static func text(_ value: Any?) -> String? {
        if let string = value as? String { return string.trimmingCharacters(in: .whitespacesAndNewlines) }
        if let number = value as? NSNumber, CFGetTypeID(number) != CFBooleanGetTypeID() { return number.stringValue }
        return nil
    }

    static func data(_ raw: Any) throws -> Any {
        guard let object = raw as? [String: Any] else { return raw }
        if let code = text(object["messageCode"]), code != "0" {
            if ["-1", "-2", "-3", "-4", "-5", "50014"].contains(code) { throw PowerError.login }
            throw PowerError.message(text(object["message"]) ?? "缴费平台返回失败")
        }
        return object["data"] ?? raw
    }

    static func confirmUser(_ raw: Any) throws {
        let value = try data(raw)
        guard let user = value as? [String: Any], let id = text(user["id"]), !id.isEmpty else { throw PowerError.login }
    }

    private static func rows(_ raw: Any) throws -> [[String: Any]] {
        let value = try data(raw)
        if let array = value as? [[String: Any]] { return array }
        if let row = value as? [String: Any] { return [row] }
        if value is NSNull { return [] }
        throw PowerError.invalidData
    }

    static func options(_ raw: Any, level: Int) throws -> [PowerOption] {
        let fields = [("id", "projectName"), ("schoolid", "schoolname"), ("buildid", "buildname"), ("roomid", "roomname")]
        let keys = fields[level]
        return try rows(raw).compactMap { row in
            if level == 0, !["electric", "electricNewCapec"].contains(text(row["proModelUrl"]) ?? "") { return nil }
            guard let id = text(row[keys.0]), !id.isEmpty, let name = text(row[keys.1]) else { throw PowerError.invalidData }
            return PowerOption(id: id, name: name.isEmpty ? id : name)
        }
    }

    static func optionRequest(_ selection: [PowerOption]) -> (path: String, form: [String: String]?) {
        if selection.isEmpty { return (projects, nil) }
        let paths = ["querySchoolList", "queryBuildingList", "queryRoomList"]
        let keys = ["projectId", "areaid", "buildid"]
        return (controller + paths[selection.count - 1], Dictionary(uniqueKeysWithValues: zip(keys, selection.map(\.id))))
    }

    static func remaining(_ raw: Any) throws -> Decimal {
        guard let row = try rows(raw).first else { throw PowerError.noReading }
        func number(_ value: Any?) throws -> Decimal {
            guard let string = text(value) else { throw PowerError.invalidData }
            guard string.range(of: #"^[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?$"#, options: .regularExpression) != nil,
                  let amount = Decimal(string: string, locale: Locale(identifier: "en_US_POSIX"))
            else { throw PowerError.invalidData }
            return amount
        }
        let amount = try number(row["oddl"])
        var result = amount
        if let total = row["suml"], !(total is NSNull) { result = try number(total) - amount }
        var rounded = Decimal()
        NSDecimalRound(&rounded, &result, 2, .plain)
        return rounded
    }
}
