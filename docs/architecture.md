# 一校通架构说明

## 当前结构

工程只面向 Android 手机，Gradle 仅包含 `:shared` 和 `:androidApp`。`shared/commonMain` 放置 Android 应用内部共用的课程表、导航、数据和 UI；`shared/androidMain` 与 `androidApp` 提供 Android 平台能力和 XATU 服务。没有 iOS 或桌面应用 target。

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

页面触发事件，repository 负责缓存和刷新，service 负责网络请求与解析。页面不直接创建网络客户端；XATU 的 CookieManager 和 OkHttpClient 由 Android DI 容器统一提供，也不把服务端响应对象当作 UI 状态长期保存。

学术页面由 `XatuAcademicViewModel` 持有成绩、考试、学业方案和学期选择状态。页面只渲染 `StateFlow` 并发送学期选择事件；ViewModel 在新页面请求、账号或学期改变时取消旧请求，服务页离开组合时也会取消当前请求，避免页面切走后继续联网或更新无关状态。

账号切换时必须同时处理对应的校园快照、学术缓存和 WebView 会话。切换学号或显式退出登录时清理 Cookie；普通 WebView 页面切换保留会话。

## 新增 XATU 服务

1. 在 `xatu/<domain>` 增加模型、service 和 parser。
2. 在 repository 中定义账号隔离的缓存读写。
3. 在 `XatuServicePage` 增加明确的 `UiState` 和用户事件。
4. 在首页增加入口，并为网络失败、空数据和会话失效提供可操作反馈。
5. 为纯解析逻辑补充 Android host test，再运行 Android 编译和 Release 构建。

当前不拆分新的 Gradle module。只有当 XATU 业务需要独立发布或依赖边界无法在 source set 内维持时才评估拆分。
