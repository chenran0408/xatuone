import SwiftUI
import WebKit

@main
struct XatuOneApp: App {
    @StateObject private var session = CampusSession()
    var body: some Scene {
        WindowGroup {
            TabView {
                NavigationStack { ScheduleScreen() }
                    .tabItem { Label("课程", systemImage: "calendar") }
                NavigationStack { AcademicScreen() }
                    .tabItem { Label("教务", systemImage: "graduationcap") }
                NavigationStack { CampusScreen() }
                    .tabItem { Label("校园", systemImage: "building.2") }
                NavigationStack { AccountScreen() }
                    .tabItem { Label("我的", systemImage: "person.crop.circle") }
            }
            .environmentObject(session)
            .sheet(item: $session.browser, onDismiss: {
                if session.browser == nil { session.dismissBrowser() }
            }) { page in
                NavigationStack { BrowserScreen(page: page) }
                    .environmentObject(session)
            }
        }
    }
}

private struct AccountScreen: View {
    @EnvironmentObject var session: CampusSession
    @State private var confirmLogout = false
    var body: some View {
        List {
            Section("教务账号") {
                Label(session.accountID.isEmpty ? "尚未登录" : "已有教务账号", systemImage: "person.crop.circle")
                Button(session.accountID.isEmpty ? "登录教务系统" : "重新认证") { session.openAcademic() }
            }
            Section {
                Button("退出并清除登录状态", role: .destructive) { confirmLogout = true }
            }
        }
        .disabled(session.signingOut)
        .navigationTitle("我的")
        .confirmationDialog("清除本机教务和校园网页登录状态？", isPresented: $confirmLogout, titleVisibility: .visible) {
            Button("退出登录", role: .destructive) { Task { await session.logout() } }
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
