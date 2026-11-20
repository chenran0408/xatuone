import Foundation
import Security

enum BackupIssue: LocalizedError {
    case invalid, address, http(Int), credential(OSStatus)
    var errorDescription: String? {
        switch self {
        case .invalid: return "备份文件版本或内容无效"
        case .address: return "请输入包含文件名的 HTTPS 备份地址"
        case .http(let status): return "WebDAV 返回 HTTP \(status)"
        case .credential(let status): return "无法访问钥匙串（\(status)）"
        }
    }
}

struct CampusBackup: Codable {
    let format: String
    let version: Int
    let created: Date
    let archive: AccountTimetables
    let appearance: [String:String]
    let reminders: CourseReminderPreferences?

    static let keys = ["xatu.appearance.mode", "xatu.appearance.style", "xatu.start.tab"]
    init(archive: AccountTimetables, defaults: UserDefaults = .standard) {
        format = "xatuone-iphone"; version = 1; created = Date()
        self.archive = archive
        appearance = Dictionary(uniqueKeysWithValues:Self.keys.compactMap { key in defaults.string(forKey:key).map { (key,$0) } })
        reminders = CourseReminderPreferences(defaults: defaults)
    }
    static func read(_ bytes: Data) throws -> Self {
        let value = try JSONDecoder().decode(Self.self,from:bytes)
        guard value.format == "xatuone-iphone", value.version == 1,
              value.archive.selected.map({value.archive.semesters[$0] != nil}) ?? true else { throw BackupIssue.invalid }
        if let reminders = value.reminders, !CourseReminderPreferences.minuteOptions.contains(reminders.minutes) {
            throw BackupIssue.invalid
        }
        for (key, table) in value.archive.semesters {
            guard !key.isEmpty else { throw BackupIssue.invalid }
            try CourseExchange.validate(table)
        }
        return value
    }
    func restore(owner: String, store: TimetableStore = .init(), defaults: UserDefaults = .standard) throws {
        try store.save(archive,account:owner)
        for key in Self.keys { defaults.set(appearance[key], forKey:key) }
        reminders?.save(to: defaults)
    }
}

struct DAVCredential: Codable, Equatable {
    var address = ""
    var user = ""
    var password = ""

    func request(method: String) throws -> URLRequest {
        guard let url = URL(string:address.trimmingCharacters(in:.whitespacesAndNewlines)),
              url.scheme?.lowercased() == "https", url.host != nil, url.user == nil, url.password == nil,
              url.query == nil, url.fragment == nil, !url.path.isEmpty, !url.absoluteString.hasSuffix("/") else { throw BackupIssue.address }
        var request = URLRequest(url:url,cachePolicy:.reloadIgnoringLocalCacheData,timeoutInterval:30)
        request.httpMethod = method
        if !user.isEmpty {
            request.setValue("Basic " + Data((user + ":" + password).utf8).base64EncodedString(), forHTTPHeaderField:"Authorization")
        }
        return request
    }

    private static func query(_ owner: String) -> [String:Any] {
        [kSecClass as String:kSecClassGenericPassword,kSecAttrService as String:"com.xatuone.campus.webdav",kSecAttrAccount as String:owner]
    }
    static func load(_ owner: String) throws -> Self? {
        var query = Self.query(owner)
        query[kSecReturnData as String] = true
        var value: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary,&value)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = value as? Data else { throw BackupIssue.credential(status) }
        return try JSONDecoder().decode(Self.self,from:data)
    }
    static func remove(_ owner: String) throws {
        let status = SecItemDelete(query(owner) as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else { throw BackupIssue.credential(status) }
    }
    func save(_ owner: String) throws {
        _ = try request(method:"GET")
        let query = Self.query(owner), data = try JSONEncoder().encode(self)
        let update = SecItemUpdate(query as CFDictionary, [kSecValueData as String:data] as CFDictionary)
        if update == errSecItemNotFound {
            var item = query
            item[kSecValueData as String] = data
            item[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            let status = SecItemAdd(item as CFDictionary,nil)
            guard status == errSecSuccess else { throw BackupIssue.credential(status) }
        } else if update != errSecSuccess { throw BackupIssue.credential(update) }
    }
}

private final class DAVRedirectPolicy: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}

enum CampusDAV {
    static func transfer(_ server: DAVCredential, upload: Data?) async throws -> Data {
        var request = try server.request(method: upload == nil ? "GET" : "PUT")
        if let upload {
            request.httpBody = upload
            request.setValue("application/json",forHTTPHeaderField:"Content-Type")
        }
        let config = URLSessionConfiguration.ephemeral
        config.httpCookieStorage = nil; config.urlCredentialStorage = nil
        let session = URLSession(configuration:config,delegate:DAVRedirectPolicy(),delegateQueue:nil)
        defer { session.finishTasksAndInvalidate() }
        let (data,response) = try await session.data(for:request)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        guard (200..<300).contains(http.statusCode) else { throw BackupIssue.http(http.statusCode) }
        return data
    }
}
