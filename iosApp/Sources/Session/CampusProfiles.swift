import Foundation

// Cookies and website data stay in WebKit. This directory only maps a verified school identity to its store.
struct CampusProfile: Identifiable, Equatable {
    let id: String
    var label: String
    let storeID: UUID?
}

struct CampusProfiles {
    private static let key = "xatu.eams.profiles"
    private(set) var entries: [CampusProfile]

    init(defaults: UserDefaults, legacy: String) {
        let saved = defaults.array(forKey: Self.key) as? [[String:String]] ?? []
        entries = saved.compactMap { row in
            guard let id = row["id"], let label = row["label"] else { return nil }
            if let value = row["store"] {
                guard let uuid = UUID(uuidString: value) else { return nil }
                return CampusProfile(id: id, label: label, storeID: uuid)
            }
            return CampusProfile(id: id, label: label, storeID: nil)
        }
        if !legacy.isEmpty && entries.isEmpty {
            remember(id: legacy, storeID: nil)
            save(defaults)
        }
    }
    mutating func remember(id: String, storeID: UUID?) {
        guard !entries.contains(where: { $0.id == id }) else { return }
        var index = 1
        while entries.contains(where: { $0.label == "教务账号 \(index)" }) { index += 1 }
        entries.append(CampusProfile(id: id, label: "教务账号 \(index)", storeID: storeID))
    }
    mutating func rename(id: String, to label: String) {
        let name = label.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty, let index = entries.firstIndex(where: { $0.id == id }) else { return }
        entries[index].label = name
    }
    mutating func remove(id: String) { entries.removeAll { $0.id == id } }
    func save(_ defaults: UserDefaults) {
        defaults.set(entries.map { entry in
            var row = ["id":entry.id,"label":entry.label]
            row["store"] = entry.storeID?.uuidString
            return row
        }, forKey:Self.key)
    }
}
