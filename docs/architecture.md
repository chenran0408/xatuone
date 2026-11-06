# 一校通架构说明

## 当前结构

工程是 Kotlin Multiplatform 项目，包含 `shared`、`androidApp`、`desktopApp` 和 `iosApp`。课程表和通用设置位于 `shared/commonMain`；XATU 校园服务仅位于 `shared/androidMain`，通过 Koin 注入到 Android 页面。

```text
通用课程表
├── commonMain UI / Navigation / Room / DataStore
└── 适配脚本 WebView 桥接

XATU Android
├── ui/settings/xatu       首页和服务页
├── xatu/academic          成绩、考试、学期、教学计划
├── xatu/campus            校历、电费、签到和定位
├── data/repository        账号和校园快照
└── ui/schoolselection/web 登录 WebView 和 Cookie 会话
```

## 数据流约定

页面触发事件，repository 负责缓存和刷新，service 负责网络请求与解析。页面不直接创建网络客户端，也不把服务端响应对象当作 UI 状态长期保存。

账号切换时必须同时处理对应的校园快照、学术缓存和 WebView 会话。WebView 页面切换保留会话，显式退出登录才清理 Cookie。

## 新增 XATU 服务

1. 在 `xatu/<domain>` 增加模型、service 和 parser。
2. 在 repository 中定义账号隔离的缓存读写。
3. 在 `XatuServicePage` 增加明确的 `UiState` 和用户事件。
4. 在首页增加入口，并为网络失败、空数据和会话失效提供可操作反馈。
5. 为纯解析逻辑补充 Android host test，再运行 Android 编译和 Release 构建。

当前不拆分新的 Gradle module。只有当 XATU 业务需要独立发布或依赖边界无法在 source set 内维持时才评估拆分。
