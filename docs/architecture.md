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

一级导航为首页、课程、教务、校园、我的。教务与校园分别登记服务，详情仍复用 `Destination.XatuService`；课程顶部集中今日安排、同步和课表设置，课表配置不再与“我的”的全局设置混排。首页课程摘要复用 `TodayScheduleViewModel`，教务摘要由 `XatuHomeViewModel` 读取既有学术 repository，按账号变化取消旧任务，展示缓存时间并提供刷新；电量摘要观察已有校园快照；首页首次创建和宿舍改变时查询一次，手动刷新复用 CampusService，写回详情页同一 Repository。按宿舍变化取消旧请求，失败保留原记录与查询时间。分钟计时仅更新课程展示，跨日才刷新教务摘要。三种 UI 风格共用这些路由和状态。首页通过导航组件实际测得的底栏高度限制滚动视口，不用固定字号倍数猜测底部安全空间。

账号切换时必须同时处理对应的校园快照、学术缓存和 WebView 会话。切换学号或显式退出登录时清理 Cookie；普通 WebView 页面切换保留会话。

## 新增 XATU 服务

1. 在 `xatu/<domain>` 增加模型、service 和 parser。
2. 在 repository 中定义账号隔离的缓存读写。
3. 在 `XatuServicePage` 增加明确的 `UiState` 和用户事件。
4. 在 `XatuPortalScreens.android.kt` 的教务或校园目录按归属登记真实入口；首页仅放有实际价值的数据摘要。为网络失败、空数据和会话失效提供可操作反馈。
5. 为纯解析逻辑补充 Android host test，再运行 Android 编译和 Release 构建。

当前不拆分新的 Gradle module。只有当 XATU 业务需要独立发布或依赖边界无法在 source set 内维持时才评估拆分。

## 运行时视觉风格

`AppSettingsModel.uiStyle` 是 Material 3 / MIUIX / COUI 的唯一偏好来源。用户在「我的 → 外观与主题」选择，`AppSettingsRepository.updateUiStyle` 只写入 `datastore/app_settings.preferences_pb` 的 `ui_style`，应用观察原有设置 Flow 即时更新；明暗模式、动态取色和自定义主色继续独立生效。

应用级 `ShiguangScheduleTheme` 保持 Material、MIUIX、COUI 的 provider 树和导航内容位置固定。共享组件只根据 `LocalUiStyle` 选择外壳，`UiCard` 用 movable content 保留外壳内的组合状态。Repository、Service、ViewModel、账号、缓存、WebView controller 和导航目的地均不以风格为键，也不创建三份业务实现。

`UiStyleComponents`、`UiFormComponents` 和 `UiSelectionComponents` 接收相同内容、受控状态及回调；Android actual 中接入各库。未使用库专用组件的深层页面继续使用同一份 Material 控件，通过应用级中性色、强调色和圆角映射适配所选风格。共享输入、弹窗和日期/颜色选择器不声称全部为 MIUIX/COUI 原生组件；校方网页、系统权限页和桌面小组件不属于 Compose 换肤范围。

COUI 是 Android Compose AAR，当前使用 `io.github.itoseo:coui:0.0.1-alpha7`，要求 Android 12+、compileSdk 37、AGP 9.1.0+。只在 androidMain 引入，复用宿主 Compose 运行时；升级依赖后应重新运行编译、host tests、lint 和 R8，并在设备上检查输入、触控、动画及会话保留。详细兼容性依据及验证边界见 `docs/ui-style-regression-2026-11-07.md` 文末。

## 课程查看与同步（2026-11-07）

- `WeeklyScheduleScreen` 持有日／周模式和原有周分页状态；日模式嵌入现有 `TodayScheduleScreen`。`TodayScheduleViewModel` 持有所选日期（未指定时跟随今天），复用原日期到周次算法，按所选日期读取课程及有效作息。首页继续独立观察自己的今日状态。
- 日期、总周数、作息归当前课表配置，颜色及教师／地点／序号显隐归原 `StyleSettingsRepository`。`schedule_style.proto` 仅追加 `hide_course_number = 29`；旧值默认 false。课程数据库、课程导入格式和 XATU 脚本未变更；`CourseDisplayName` 只在展示时拆分脚本既有的数字序号后缀。
- `XatuCourseSyncContextViewModel` 复用账号和教务 repository 确认当前学期；实际导入学期仍由原脚本询问。无法确认时展示真实错误，不按月份补造。页面说明本地账号与网页当前会话的区别，账号管理仍使用既有入口。
- `WebBridgeHandler.importProgress` 是单次同步进度来源。XATU 执行包装仅观察原异步脚本返回，沿用原公共桥接；保存课程成功且收到完成通知才展示同步完成。脚本提前返回、请求错误和原保存错误持续显示在导入页，执行期间禁止重复点击。其他适配器保留原完成导航。
- 页面内容不以风格为键；日／周切换不清 Cookie、不重建导航或导入 WebView。底栏测量值通过更新状态传入保留的导航条目，避免首次打开的课表使用过期零边距。


### 设置归属（2026-11-07）

`SettingsHomeScreen` 仅管理六个分类入口，`SettingsScreen` 现在只处理当前课表日期/周次。`AcademicSettings`、`CampusSettings` 为可恢复的子路由；Android 的 `XatuPreferencesScreen` 观察原账号和校园 repository，查询链接继续使用原服务页。账号管理统一由 `XatuLoginScreen` 承担，退出仍调用原 `XatuSessionRepository.clear()`，只增加用户确认。`electricitySettings` 与 `electricity` 分离配置与查询界面，继续共享 `XatuCampusRepository.snapshot`、原 API 和存储格式。外观仍由原 AppSettings 持久化，不新增业务状态副本或清会话行为。

### 电费查询状态与记录（2026-11-07）

- `XatuCampusService` 保留缴费平台原请求、参数和 Cookie 来源；`XatuElectricityResponse` 统一计算剩余电量（接口 kWh 与“度”等价），区分平台会话失效、无电量和其他错误。缴费请求禁用自动重定向以识别登录跳转，沿用原 TLS 校验。
- `XatuElectricityRepository` 单例持有首页与详情共同的查询状态；已有 `XatuCampusRepository` 仍持久化宿舍、电量、时间和真实查询记录，存储格式不变。同房间查询进行中不重复请求；旧请求不能覆盖新房间反馈，保存沿用原房间一致性校验。
- 页面明确区分缓存、查询中、成功、失败与登录失效。最近查询时间仅在真实请求成功并保存时更新。趋势仅使用已保存记录，少于两条不绘制曲线，不将查询间隔解释为逐日用电账单。
- `XatuElectricityPage.android.kt` 管理展示与房间草稿选择；项目、校区、楼栋、房间逐级请求，点击保存才修改持久化房间。初始化先等待校园快照读完，避免把未读取状态误作未绑定。实际换房仍沿用原清除旧房间本机缓存与记录规则。

### 核心页面的共享视觉规则（2026-11-07）

`UiPageLayout.kt` 集中核心页面实际复用的尺寸（16dp 页边距、8dp 内容间距、24dp 图标、48dp 触控目标和 840dp 内容宽度上限）、列表分隔线和加载/空/错误/成功反馈。服务页、首页、设置、课程工具和今日课程限制正文宽度；周课表网格保留用户配置与原横向布局。

### 原生风格页面组织（2026-11-07）

- 业务仍由原 Service/Repository、ViewModel 与 DataStore 持有，风格分支不发起请求、不保存业务副本。`UiScaffold` 保留稳定的布局槽位，`UiTopAppBar` 分派原库标题；页面内可直接使用平台库组件，不能再以同一 Row 加配色冒充三库实现。
- 30 个现有页面接入稳定 Scaffold 布局：只用其定位正文、顶栏、底栏及浮动操作；顶栏分别使用 Material TopAppBar、MIUIX TopAppBar/SmallTopAppBar、COUI TopAppBar/LargeTopAppBar，大标题接入各库折叠滚动连接。没有复制业务页面。未切换 Scaffold 的组合 owner，避免当前 Compose runtime 跨原库 SubcomposeLayout 搬移嵌套内容时崩溃。
- 一级导航复用同一目的地列表与 NavDisplay：Material NavigationBar/NavigationRail；MIUIX NavigationBar/NavigationRail；COUI API 34+ 浮动 NavigationBar，API 31–33 或大字号使用 COUI Item 构成可滚动入口。COUI 无公开侧栏组件，宽屏使用其 Item 侧栏，不调用内部 API。alpha7 原生 Dialog 的二进制 ABI 依赖按其发布 BOM 2026.08.01 的 UI/动画/Foundation版本对齐（未整体导入 BOM，保留既有 Material3），不添加新的 UI 组件库。
- 门户对同一服务清单分别呈现 Material 双列卡片、MIUIX 图标列表与 COUI Group/Item 分组。首页共用摘要数据和操作清单：Material 摘要+快捷按钮，MIUIX 高频列表前置，COUI 课程之后为分组快捷服务。
- `UiSettingRow` 分别分派 ListItem/BasicComponent/Item。`UiCard` 分派 Card/Card/Group。`UiText` 不再以 Material bodyLarge 覆盖库排版，旧页面的排版角色映射为各库指标，COUI 能表达的角色直接使用 COUI Text；显式课表字号/行高等仍使用可配置 Foundation 文本。
- `UiDialogAction` 仅含文字、回调与 enabled，用于原生弹窗动作，不携带业务逻辑。弹窗分派 Material AlertDialog、MIUIX WindowDialog、COUI Dialog；底部面板分派 ModalBottomSheet、WindowBottomSheet 和 COUI 底部 Dialog。COUI alpha7 没有公开通用按钮、普通行内 TextField 或可拖动 BottomSheet：页面动作使用 Item；输入使用 Item 中的 Foundation 编辑框，保留键盘选项、焦点与选区；底部窗口以原生关闭动作退出，不声称支持拖拽面板。COUI 内部 DialogButton/CouiDialogWindow 不可直接引用。
- 今日日期和开学日期选择使用同一个受控日期：Material DatePicker、MIUIX NumberPicker 年/月/日滚轮、COUI CalendarPicker。周次等小网格只放原库选中标记，避免按宽列表测量导致文字消失。
- 主题 provider、导航 owner 和 WebView controller 不以风格为 key。页面正文留在固定 Scaffold 槽位，不在不同库的子组合之间搬移导航子树，表单、列表与导航状态仍由原调用方管理。转场按风格分派，不修改导航提交逻辑。
- 课表网格、课程色、图表、图片、学校网页和桌面小组件继续沿用既有渲染；专用编辑器中未迁移的直接 Material 控件不是原生 MIUIX/COUI，不能将页面外壳迁移描述为每个控件均已迁移。
