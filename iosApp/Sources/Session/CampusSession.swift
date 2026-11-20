import CryptoKit
import Foundation
import SwiftUI
import WebKit

enum CampusSessionError: LocalizedError {
    case academicLoginRequired, paymentLoginRequired, invalidPage, http(Int)
    var errorDescription: String? {
        switch self {
        case .academicLoginRequired: return "请登录教务系统"
        case .paymentLoginRequired: return "请登录缴费平台"
        case .invalidPage: return "学校返回了无法识别的页面"
        case .http(let code): return "学校服务返回 HTTP \(code)"
        }
    }
}

struct CampusBrowser: Identifiable {
    enum Kind { case academic, payment, service }
    let id = UUID()
    let kind: Kind
    let title: String
    let view: WKWebView
}

private final class NoPaymentRedirect: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}

@MainActor
final class CampusSession: NSObject, ObservableObject, WKNavigationDelegate, WKUIDelegate {
    static let academicBase = URL(string: "http://jwgl2018.xatu.edu.cn/eams/")!
    static let loginURL = URL(string: "https://authserver.xatu.edu.cn/authserver/login?service=http://jwgl2018.xatu.edu.cn/eams/homeExt.action")!
    private static let payBase = URL(string: "https://jfpay.xatu.edu.cn/api/")!
    private static let profileKey = "xatu.eams.active-profile"
    @Published private(set) var accountID = UserDefaults.standard.string(forKey: CampusSession.profileKey) ?? ""
    @Published var browser: CampusBrowser?
    @Published var browserError: String?
    @Published private(set) var signingOut = false
    private(set) var webView = WKWebView(frame: .zero)
    private var verifying: Task<Void, Error>?
    private var paymentMonitor: Task<Void, Never>?
    private var paymentCompleted: (() -> Void)?
    private var sessionVersion = 0
    private var waiting: (WKNavigation, CheckedContinuation<Void, Error>)?
    private let paymentClient: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.httpShouldSetCookies = false
        config.httpCookieStorage = nil
        config.timeoutIntervalForRequest = 20
        return URLSession(configuration: config, delegate: NoPaymentRedirect(), delegateQueue: nil)
    }()

    override init() {
        super.init()
        configure(webView)
    }

    private func configure(_ view: WKWebView) {
        view.navigationDelegate = self
        view.uiDelegate = self
        view.allowsBackForwardNavigationGestures = true
    }

    static func isAcademic(_ url: URL?) -> Bool {
        guard let url else { return false }
        return ["http","https"].contains(url.scheme) && url.host?.lowercased() == academicBase.host &&
            url.port == nil && url.path.hasPrefix("/eams/")
    }

    static func academicURL(_ path: String) throws -> URL {
        guard !path.hasPrefix("/"), let u = URL(string: path, relativeTo: academicBase)?.absoluteURL,
              isAcademic(u) else { throw CampusSessionError.invalidPage }
        return u
    }

    static func accountKey(in html: String) throws -> String {
        let regex = try NSRegularExpression(pattern: #"bg\.form\.addInput\(\s*form\s*,\s*["']ids["']\s*,\s*["'](\d+)["']"#)
        guard let match = regex.firstMatch(in: html, range: NSRange(html.startIndex..., in: html)),
              let range = Range(match.range(at: 1), in: html) else { throw CampusSessionError.academicLoginRequired }
        return SHA256.hash(data: Data(html[range].utf8)).map { String(format: "%02x", $0) }.joined()
    }

    func ensureAcademic() async throws {
        guard !signingOut else { throw CancellationError() }
        if let verifying { return try await verifying.value }
        let version = sessionVersion
        let task = Task { @MainActor in
            if !Self.isAcademic(webView.url) { try await navigateToLogin() }
            guard Self.isAcademic(webView.url) else { throw CampusSessionError.academicLoginRequired }
            let page = try await academicHTML("courseTableForStd.action?sf_request_type=ajax")
            try Task.checkCancellation()
            guard version == sessionVersion else { throw CancellationError() }
            let id = try Self.accountKey(in: page)
            if accountID != id {
                UserDefaults.standard.set(id, forKey: Self.profileKey)
                accountID = id
            }
        }
        verifying = task
        defer { if version == sessionVersion { verifying = nil } }
        try await task.value
    }

    private func navigateToLogin() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            guard let nav = webView.load(URLRequest(url: Self.loginURL, timeoutInterval: 25)) else {
                continuation.resume(throwing: CampusSessionError.invalidPage); return
            }
            waiting?.1.resume(throwing: CancellationError())
            waiting = (nav, continuation)
        }
    }

    func evaluate(_ script: String) async throws -> Any? {
        guard Self.isAcademic(webView.url) else { throw CampusSessionError.academicLoginRequired }
        return try await webView.evaluateJavaScript(script)
    }

    func academicHTML(_ path: String, form: [String:String]? = nil) async throws -> String {
        guard Self.isAcademic(webView.url) else { throw CampusSessionError.academicLoginRequired }
        let url = try Self.academicURL(path)
        let args: [String:Any] = ["target":url.absoluteString, "fields": form.map { $0 as Any } ?? NSNull()]
        let result = try await webView.callAsyncJavaScript("""
            try {
                const init = {credentials:'same-origin',cache:'no-store'};
                if (fields !== null) { init.method='POST'; init.body=new URLSearchParams(fields); }
                const r = await fetch(target, init);
                return {status:r.status,url:r.url,text:await r.text()};
            } catch (e) { return {network:true}; }
            """, arguments: args, in: nil, contentWorld: .page)
        guard let row = result as? [String:Any] else { throw CampusSessionError.invalidPage }
        if row["network"] as? Bool == true { throw URLError(.cannotConnectToHost) }
        guard let end = row["url"] as? String, Self.isAcademic(URL(string:end)),
              let html = row["text"] as? String else { throw CampusSessionError.academicLoginRequired }
        let status = row["status"] as? Int ?? 0
        if status == 401 { throw CampusSessionError.academicLoginRequired }
        guard (200..<300).contains(status) else { throw CampusSessionError.http(status) }
        if html.range(of: #"<input[^>]+type\s*=\s*["']password["']"#, options:[.regularExpression,.caseInsensitive]) != nil {
            throw CampusSessionError.academicLoginRequired
        }
        return html
    }

    func openAcademic() {
        guard !signingOut else { return }
        dismissBrowser()
        browser = CampusBrowser(kind: .academic, title: "教务登录", view: webView)
        webView.load(URLRequest(url: Self.loginURL))
    }

    func finishAcademicLogin() async {
        guard let page = browser, page.kind == .academic else { return }
        do {
            guard Self.isAcademic(webView.url) else { throw CampusSessionError.academicLoginRequired }
            try await ensureAcademic()
            if browser?.id == page.id { dismissBrowser() }
        } catch {
            if browser?.id == page.id { browserError = error.localizedDescription }
        }
    }

    func open(_ url: URL, title: String) {
        presentService(url, title: title, kind: .service)
    }

    func openPayment(onComplete: @escaping () -> Void) {
        presentService(URL(string: "https://jfpay.xatu.edu.cn/casLogin/")!, title: "缴费平台登录", kind: .payment)
        if browser?.kind == .payment { paymentCompleted = onComplete }
    }

    private func presentService(_ url: URL, title: String, kind: CampusBrowser.Kind) {
        guard !signingOut, ["https","http"].contains(url.scheme) else { return }
        dismissBrowser()
        let view = WKWebView(frame:.zero)
        configure(view)
        browser = CampusBrowser(kind:kind,title:title,view:view)
        view.load(URLRequest(url:url))
    }

    func dismissBrowser() {
        paymentMonitor?.cancel()
        paymentMonitor = nil
        paymentCompleted = nil
        if browser?.kind != .academic { browser?.view.stopLoading() }
        browser = nil
        browserError = nil
    }

    private func paymentToken(for url: URL) async -> String? {
        let cookies = await WKWebsiteDataStore.default().httpCookieStore.allCookies()
        return cookies.filter {
            let domain = $0.domain.lowercased()
            let host = url.host ?? ""
            let matchesHost = host == domain || (domain.hasPrefix(".") && (host == String(domain.dropFirst()) || host.hasSuffix(domain)))
            let matchesPath = url.path == $0.path || url.path.hasPrefix($0.path.hasSuffix("/") ? $0.path : $0.path + "/")
            return $0.name == "datalook_reimbursement_token" && matchesHost && matchesPath &&
                !$0.value.isEmpty && ($0.expiresDate.map { $0 > Date() } ?? true)
        }.sorted { $0.path.count > $1.path.count }.first?.value
    }

    func paymentJSON(_ path: String, form: [String:String]? = nil) async throws -> Any {
        let version = sessionVersion
        guard !signingOut, let url = URL(string:path,relativeTo:Self.payBase)?.absoluteURL,
              url.scheme == "https", url.host == Self.payBase.host, url.port == nil, url.path.hasPrefix("/api/") else {
            throw CampusSessionError.invalidPage
        }
        guard let token = await paymentToken(for:url) else { throw CampusSessionError.paymentLoginRequired }
        var request = URLRequest(url:url)
        request.setValue(token,forHTTPHeaderField:"X-Token")
        request.setValue("application/json",forHTTPHeaderField:"Accept")
        if let form {
            request.httpMethod = "POST"
            request.setValue("application/json; charset=utf-8",forHTTPHeaderField:"Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject:form)
        }
        let (data,response) = try await paymentClient.data(for:request)
        guard version == sessionVersion else { throw CancellationError() }
        guard let http = response as? HTTPURLResponse else { throw CampusSessionError.invalidPage }
        if http.statusCode == 401 || (300..<400).contains(http.statusCode) { throw CampusSessionError.paymentLoginRequired }
        guard (200..<300).contains(http.statusCode) else { throw CampusSessionError.http(http.statusCode) }
        return try JSONSerialization.jsonObject(with:data)
    }

    private func observePaymentLogin(_ page: CampusBrowser) {
        paymentMonitor?.cancel()
        paymentMonitor = Task {
            var lastToken: String?
            while !Task.isCancelled && browser?.id == page.id {
                guard page.view.url?.host == Self.payBase.host else { return }
                if let token = await paymentToken(for:Self.payBase.appendingPathComponent("pay/queryUserInfo")), token != lastToken {
                    lastToken = token
                    do {
                        let json = try await paymentJSON("pay/queryUserInfo") as? [String:Any]
                        if let user = json?["data"] as? [String:Any], let id = user["id"],
                           !(id is NSNull), !String(describing:id).isEmpty,
                           !Task.isCancelled, browser?.id == page.id {
                            let done = paymentCompleted
                            dismissBrowser()
                            done?()
                            return
                        }
                    } catch {
                        if !Task.isCancelled && browser?.id == page.id { browserError = error.localizedDescription }
                    }
                }
                do { try await Task.sleep(nanoseconds:750_000_000) } catch { return }
            }
        }
    }

    func logout() async {
        guard !signingOut else { return }
        signingOut = true
        sessionVersion += 1
        verifying?.cancel(); verifying = nil
        waiting?.1.resume(throwing:CancellationError()); waiting = nil
        dismissBrowser()
        webView.stopLoading()
        webView.navigationDelegate = nil
        webView = WKWebView(frame:.zero)
        configure(webView)
        UserDefaults.standard.removeObject(forKey:Self.profileKey)
        accountID = ""
        let store = WKWebsiteDataStore.default()
        await store.removeData(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(), modifiedSince: .distantPast)
        signingOut = false
    }

    func webView(_ view: WKWebView, didFinish navigation: WKNavigation!) {
        if view === webView, let current = waiting, current.0 === navigation {
            waiting = nil; current.1.resume()
        }
        guard let page = browser, page.view === view else { return }
        browserError = nil
        if page.kind == .academic && Self.isAcademic(view.url) { Task { await finishAcademicLogin() } }
        if page.kind == .payment { observePaymentLogin(page) }
    }

    private func failed(_ view: WKWebView,_ navigation: WKNavigation?, _ error: Error) {
        if view === webView, let current = waiting, current.0 === navigation {
            waiting = nil; current.1.resume(throwing:error)
        }
        if browser?.view === view && (error as NSError).code != NSURLErrorCancelled { browserError = error.localizedDescription }
    }
    func webView(_ view: WKWebView, didFail navigation: WKNavigation!, withError error: Error) { failed(view,navigation,error) }
    func webView(_ view: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) { failed(view,navigation,error) }

    func webView(_ view: WKWebView, decidePolicyFor action: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = action.request.url else { decisionHandler(.cancel); return }
        if url.scheme == "http" && url.host == "authserver.xatu.edu.cn" && action.request.httpMethod == "GET",
           var parts = URLComponents(url:url,resolvingAgainstBaseURL:false) {
            parts.scheme = "https"
            if let secure = parts.url {
                let pending = view === webView ? waiting : nil
                decisionHandler(.cancel)
                if let nav = view.load(URLRequest(url:secure)), let pending { waiting = (nav,pending.1) }
                return
            }
        }
        decisionHandler(.allow)
    }

    func webView(_ view: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if action.targetFrame == nil { view.load(action.request) }
        return nil
    }
}
