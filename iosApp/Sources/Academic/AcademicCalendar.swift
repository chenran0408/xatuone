import Foundation
import SwiftUI
import Vision

struct AcademicCalendarPage: Codable {
    static let index = URL(string: "https://jwcw.xatu.edu.cn/index/xagydxxl.htm")!
    let title: String
    let article: URL
    let image: URL
    let dateRanges: [String]

    static func ranges(_ text: String) -> [String] {
        let compact = text.replacingOccurrences(of: #"\s+"#, with: "", options: .regularExpression)
        let matches = AcademicParser.captures(#"(\d{4})年(\d{1,2})月(\d{1,2})日[-－—–~～至到]+(?:(\d{4})年)?(\d{1,2})月(\d{1,2})日"#, in: compact)
        let cal = AcademicParser.schoolCalendar
        func date(_ y: Int, _ m: Int, _ d: Int) -> Date? {
            let components = DateComponents(year: y, month: m, day: d)
            guard let value = cal.date(from: components),
                  cal.component(.year, from: value) == y, cal.component(.month, from: value) == m,
                  cal.component(.day, from: value) == d else { return nil }
            return value
        }
        var seen = Set<String>()
        return matches.compactMap { m in
            guard let year = Int(m[1]), let month = Int(m[2]), let day = Int(m[3]),
                  let endMonth = Int(m[5]), let endDay = Int(m[6]),
                  let start = date(year, month, day), let end = date(Int(m[4]) ?? year + (endMonth < month ? 1 : 0), endMonth, endDay),
                  let length = cal.dateComponents([.day], from: start, to: end).day, (30...260).contains(length) else { return nil }
            let range = "\(year)年\(month)月\(day)日 至 \(cal.component(.year, from: end))年\(endMonth)月\(endDay)日"
            return seen.insert(range).inserted ? range : nil
        }
    }

    static func link(_ raw: String, relativeTo base: URL) -> URL? {
        guard !raw.isEmpty, let url = URL(string: raw, relativeTo: base)?.absoluteURL,
              url.scheme?.lowercased() == "https", url.host?.lowercased() == index.host else { return nil }
        return url
    }

    static func fetch(_ url: URL, referer: URL) async throws -> Data {
        var request = URLRequest(url: url)
        request.setValue(referer.absoluteString, forHTTPHeaderField: "Referer")
        let (bytes, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw AcademicIssue("校历响应异常") }
        if http.statusCode == 403 || http.url?.path.contains("/resource/code/auth/") == true {
            throw AcademicIssue("校历访问受限，请连接校园网")
        }
        guard (200..<300).contains(http.statusCode) else { throw AcademicIssue("校历请求失败（HTTP \(http.statusCode)）") }
        return bytes
    }

    @MainActor static func load() async throws -> Self {
        func document(_ url: URL, referer: URL) async throws -> AcademicDocument {
            let bytes = try await fetch(url, referer: referer)
            guard let html = String(data: bytes, encoding: .utf8) else { throw AcademicIssue("校历网页编码异常") }
            return try await AcademicDocument.read(html)
        }
        let list = try await document(index, referer: index)
        let entry = list.links.first { a in
            a.text.contains("校历") && link(a.url, relativeTo: index).map { $0.path.contains("/info/") || $0.path == "/news/content.jsp" } == true
        }
        guard let entry, let article = link(entry.url, relativeTo: index) else { throw AcademicIssue("未找到最新校历") }
        let detail = try await document(article, referer: article)
        let picture = detail.images.first { $0.source.contains("__local") } ?? detail.images.first { !$0.original.isEmpty } ??
            detail.images.first { img in
                let src = img.source.lowercased()
                return src.range(of: #"\.(jpg|jpeg|png)(?:\?|$)"#, options: .regularExpression) != nil &&
                    !["logo", "nav-", "bottom-icon"].contains(where: src.contains)
            }
        guard let picture, let url = link(picture.original.isEmpty ? picture.source : picture.original, relativeTo: article) else {
            throw AcademicIssue("校历正文未提供原图")
        }
        return Self(title: entry.text, article: article, image: url, dateRanges: ranges(detail.text))
    }

    static func recognize(_ bytes: Data) async throws -> String {
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        let languages = try request.supportedRecognitionLanguages()
        let chinese = languages.first { $0.hasPrefix("zh-Hans") }
        guard let chinese else { throw AcademicIssue("此设备不支持中文文字识别") }
        request.recognitionLanguages = [chinese] + languages.filter { $0 == "en-US" }
        request.usesLanguageCorrection = true
        try VNImageRequestHandler(data: bytes, options: [:]).perform([request])
        return (request.results ?? []).compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
    }
}

struct AcademicCalendarCopy: Codable {
    let page: AcademicCalendarPage
    let original: Data
    let updatedAt: Date
    var recognized: String?

    private static func file(_ directory: URL?) throws -> URL {
        let root = try directory ?? FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        return root.appendingPathComponent("xatu-public-calendar.plist")
    }

    static func load(directory: URL? = nil) throws -> Self? {
        let url = try file(directory)
        guard FileManager.default.fileExists(atPath: url.path) else { return nil }
        return try PropertyListDecoder().decode(Self.self, from: Data(contentsOf: url))
    }

    func save(directory: URL? = nil) throws {
        let encoder = PropertyListEncoder()
        encoder.outputFormat = .binary
        try encoder.encode(self).write(to: Self.file(directory), options: .atomic)
    }
}

struct AcademicCalendarScreen: View {
    @EnvironmentObject private var session: CampusSession
    @State private var copy: AcademicCalendarCopy?
    @State private var image: UIImage?
    @State private var error: String?
    @State private var loading = true
    @State private var scanning = false
    @State private var scanError: String?
    @State private var scanID = 0
    @State private var showImage = false
    @State private var revision = 0

    var body: some View {
        List {
            if let copy {
                let page = copy.page
                Section(page.title) {
                    let ranges = page.dateRanges.isEmpty ? AcademicCalendarPage.ranges(copy.recognized ?? "") : page.dateRanges
                    ForEach(ranges, id: \.self) { Text($0) }
                    LabeledContent("上次更新", value: copy.updatedAt.formatted(date: .abbreviated, time: .shortened))
                    if let image {
                        Button { showImage = true } label: {
                            Image(uiImage: image).resizable().scaledToFit().accessibilityLabel("查看校历原图")
                        }
                    }
                }
                Button("打开教务处原文") { session.open(page.article, title: page.title) }
            }
            if loading { ProgressView("正在读取校历") }
            if let error {
                Text(error).foregroundStyle(.secondary)
                Button("重试") { revision += 1 }.disabled(scanning)
                if copy == nil { Button("打开教务处校历") { session.open(AcademicCalendarPage.index, title: "校历") } }
            }
            if let copy {
                Section("识别文字") {
                    if scanning { ProgressView("正在识别") }
                    if let recognized = copy.recognized { Text(recognized.isEmpty ? "没有识别到文字" : recognized).textSelection(.enabled) }
                    if let scanError { Text(scanError).foregroundStyle(.secondary) }
                    if !scanning && copy.recognized == nil { Button("识别校历文字") { scanID += 1 }.disabled(loading) }
                }
            }
        }
        .navigationTitle("校历")
        .toolbar { Button { revision += 1 } label: { Label("刷新校历", systemImage: "arrow.clockwise") }.disabled(loading || scanning) }
        .task(id: revision) {
            loading = true; error = nil; scanError = nil
            // A damaged local copy must not prevent a fresh download.
            if copy == nil {
                do {
                    if let cached = try AcademicCalendarCopy.load(), let decoded = UIImage(data: cached.original) {
                        copy = cached; image = decoded
                    }
                } catch { self.error = error.localizedDescription }
            }
            do {
                let result = try await AcademicCalendarPage.load()
                try Task.checkCancellation()
                let data = try await AcademicCalendarPage.fetch(result.image, referer: result.article)
                guard let decoded = UIImage(data: data) else { throw AcademicIssue("无法打开校历原图") }
                try Task.checkCancellation()
                let cachedText = copy?.original == data ? copy?.recognized : nil
                let updated = AcademicCalendarCopy(page: result, original: data, updatedAt: Date(), recognized: cachedText)
                try updated.save()
                copy = updated; image = decoded; self.error = nil
            } catch { if Task.isCancelled { return }; self.error = error.localizedDescription }
            loading = false
        }
        .task(id: scanID) {
            guard scanID > 0, let source = copy else { scanning = false; return }
            scanning = true; scanError = nil
            do {
                let result = try await AcademicCalendarPage.recognize(source.original)
                try Task.checkCancellation()
                if var current = copy, current.original == source.original {
                    current.recognized = result
                    try current.save()
                    copy = current
                }
            } catch { if Task.isCancelled { return }; scanError = error.localizedDescription }
            scanning = false
        }
        .sheet(isPresented: $showImage) { if let image { AcademicOriginalImage(image: image) } }
    }
}

private struct AcademicOriginalImage: View {
    let image: UIImage
    @Environment(\.dismiss) private var dismiss
    @State private var scale: CGFloat = 1
    @GestureState private var pinch: CGFloat = 1
    var body: some View {
        NavigationStack {
            GeometryReader { box in
                ScrollView([.horizontal, .vertical]) {
                    Image(uiImage: image).resizable().scaledToFit()
                        .frame(width: box.size.width * min(5, max(1, scale * pinch)))
                        .accessibilityLabel("校历原图")
                        .simultaneousGesture(MagnificationGesture().updating($pinch) { value, state, _ in state = value }
                            .onEnded { scale = min(5, max(1, scale * $0)) })
                }
            }
            .navigationTitle("校历原图").navigationBarTitleDisplayMode(.inline)
            .toolbar { Button("完成") { dismiss() } }
        }
    }
}
