import SwiftUI
import WebKit

@main
struct XatuOneApp: App {
    @StateObject private var session = CampusSession()
    @Environment(\.scenePhase) private var phase
    @AppStorage("xatu.appearance.style") private var style = "system"
    @AppStorage("xatu.appearance.mode") private var mode = "auto"
    @State private var tab = CampusTab(rawValue: UserDefaults.standard.string(forKey:"xatu.start.tab") ?? "home") ?? .home
    var body: some Scene {
        WindowGroup {
            TabView(selection:$tab) {
                destination(.home) { CampusHome { tab = .courses } }
                destination(.courses) { ScheduleScreen() }
                destination(.academic) { AcademicScreen() }
                destination(.campus) { CampusScreen() }
                destination(.account) { AccountScreen() }
            }
            .safeAreaInset(edge:.bottom,spacing:0) {
                if style != "system" { CampusTabStrip(style:style,selected:$tab) }
            }
            .environment(\.defaultMinListRowHeight, style == "compact" ? 44 : 49)
            .tint(style == "compact" ? .blue : style == "floating" ? .teal : .accentColor)
            .preferredColorScheme(mode == "dark" ? .dark : mode == "light" ? .light : nil)
            .onAppear { refreshDevice() }
            .onChange(of:session.timetableOwner) { _ in refreshDevice() }
            .onChange(of:phase) { if $0 == .active { refreshDevice() } }
            .onReceive(NotificationCenter.default.publisher(for:.xatuTimetableChanged)) { note in
                if note.object as? String == session.timetableOwner { refreshDevice() }
            }
            .onReceive(NotificationCenter.default.publisher(for:UIApplication.significantTimeChangeNotification)) { _ in refreshDevice() }
            .environmentObject(session)
            .sheet(item: $session.browser, onDismiss: {
                if session.browser == nil { session.dismissBrowser() }
            }) { page in
                NavigationStack { BrowserScreen(page: page) }
                    .environmentObject(session)
            }
        }
    }
    private func refreshDevice() { CourseDeviceServices.shared.refresh(account:session.timetableOwner) }

    private func destination<V: View>(_ item: CampusTab, @ViewBuilder content: () -> V) -> some View {
        NavigationStack { content().toolbar(style == "system" ? .visible : .hidden, for:.tabBar) }
            .tabItem { Label(item.title,systemImage:item.icon) }.tag(item)
    }
}

private struct AccountScreen: View {
    @EnvironmentObject var session: CampusSession
    @State private var confirmLogout = false
    @State private var switching = false
    @State private var feedback: String?
    var body: some View {
        List {
            Section("教务账号") {
                Label(session.accountID.isEmpty ? "尚未登录" : "已有教务账号", systemImage: "person.crop.circle")
                Button(session.accountID.isEmpty ? "登录教务系统" : "重新认证") { session.openAcademic() }
                if !session.accountID.isEmpty {
                    Button("切换教务账号") { switching = true; confirmLogout = true }
                }
            }
            Section {
                NavigationLink { CampusAppearanceScreen() } label: { Label("外观与启动",systemImage:"circle.lefthalf.filled") }
                NavigationLink { CampusBackupScreen() } label: { Label("备份与恢复",systemImage:"externaldrive") }
            }
            Section {
                Button("退出并清除登录状态", role: .destructive) { switching = false; confirmLogout = true }
            }
            if let feedback { Text(feedback) }
        }
        .disabled(session.signingOut)
        .navigationTitle("我的")
        .confirmationDialog("退出当前账号并清除教务、宿舍和签到缓存？已同步课表将保留。", isPresented: $confirmLogout, titleVisibility: .visible) {
            Button(switching ? "退出并切换账号" : "退出登录", role: .destructive) {
                let openNext = switching
                Task {
                    do {
                        try await session.logout()
                        feedback = "已退出登录"
                        if openNext { session.openAcademic() }
                    } catch { feedback = error.localizedDescription }
                }
            }
            Button("取消", role: .cancel) {}
        }
    }
}

private struct BrowserView: UIViewRepresentable {
    let page: WKWebView
    func makeUIView(context: Context) -> WKWebView { page }
    func updateUIView(_ uiView: WKWebView, context: Context) {}
}

private struct BrowserScreen: View {
    let page: CampusBrowser
    @EnvironmentObject var session: CampusSession
    @State private var canGoBack = false
    var body: some View {
        VStack(spacing: 0) {
            if let error = session.browserError {
                HStack {
                    Text(error).font(.callout)
                    Spacer()
                    Button("重新加载") { session.browserError = nil; page.view.reload() }
                }.padding()
            }
            BrowserView(page:page.view)
        }
        .onReceive(page.view.publisher(for: \.canGoBack)) { canGoBack = $0 }
        .navigationTitle(page.title).navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement:.cancellationAction) { Button("关闭") { session.dismissBrowser() } }
            ToolbarItemGroup(placement:.primaryAction) {
                if canGoBack { Button { page.view.goBack() } label: { Label("后退",systemImage:"chevron.backward") } }
                if page.kind == .academic { Button("完成登录") { Task { await session.finishAcademicLogin() } } }
            }
        }
    }
}
