import SwiftUI

enum CampusTab: String, CaseIterable {
    case home, courses, academic, campus, account
    var title: String {
        switch self { case .home:return "首页"; case .courses:return "课程"; case .academic:return "教务"; case .campus:return "校园"; case .account:return "我的" }
    }
    var icon: String {
        switch self { case .home:return "house"; case .courses:return "calendar"; case .academic:return "graduationcap"; case .campus:return "building.2"; case .account:return "person.crop.circle" }
    }
}

struct CampusAppearanceScreen: View {
    @AppStorage("xatu.appearance.mode") private var mode = "auto"
    @AppStorage("xatu.appearance.style") private var style = "system"
    @AppStorage("xatu.start.tab") private var start = "home"
    var body: some View {
        Form {
            Picker("界面样式", selection:$style) {
                Text("iOS 系统").tag("system"); Text("紧凑标签栏").tag("compact"); Text("悬浮导航").tag("floating")
            }
            Picker("外观",selection:$mode) {
                Text("跟随系统").tag("auto"); Text("浅色").tag("light"); Text("深色").tag("dark")
            }
            Picker("启动页面", selection:$start) {
                ForEach(CampusTab.allCases,id:\.self) { tab in Text(tab.title).tag(tab.rawValue) }
            }
        }.navigationTitle("外观与启动")
    }
}

struct CampusTabStrip: View {
    let style: String
    @Binding var selected: CampusTab
    @Environment(\.dynamicTypeSize) private var type
    var body: some View {
        let floating = style == "floating"
        HStack(spacing:2) {
            ForEach(CampusTab.allCases,id:\.self) { item in
                let active = item == selected
                Button { selected = item } label: {
                    VStack(spacing:3) {
                        Image(systemName:item.icon).symbolVariant(active ? .fill : .none)
                        if !type.isAccessibilitySize { Text(item.title).font(.caption) }
                    }
                    .foregroundStyle(active ? Color.accentColor : Color.secondary)
                    .frame(maxWidth:.infinity,minHeight:44)
                    .background(active && floating ? Color.accentColor.opacity(0.13) : .clear,in:Capsule())
                    .overlay(alignment:.top) {
                        if !floating && active { Capsule().fill(Color.accentColor).frame(width:23,height:2) }
                    }
                }
                .buttonStyle(.plain).accessibilityLabel(item.title)
                .accessibilityIdentifier("campus.tab." + item.rawValue)
                .accessibilityAddTraits(active ? .isSelected : [])
            }
        }
        .padding(floating ? 4 : 1)
        .background(.regularMaterial,in:RoundedRectangle(cornerRadius:floating ? 28 : 0))
        .padding(.horizontal,floating ? 12 : 0).padding(.bottom,floating ? 3 : 0)
    }
}
