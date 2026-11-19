# 三种 UI 风格最终回归记录

> 以下原始回归记录为此前阶段的快照；后续前后端一致性修正及最新验证见文末最新章节。旧的设置路径、主色限制、开关实现和页面覆盖描述已由该节更新，不代表当前实现。

日期：2026-11-07。仓库：`.`，分支 `main`，HEAD `48ed6cf`，领先远程 2 个提交。

结论：Android 编译、现有 host tests、Debug/Release lint、R8 Release 构建通过；仍未达到全页面、实体设备验收完成的标准。本轮只新增此报告，没有修改业务源码、配置或依赖，没有提交、推送、发布或安装。

## 切换与状态所有权

- 路径：设置 → 更多选项 → 主题设置 → 界面风格。三选一回调进入 `SettingsViewModel.onUiStyleChanged`，写入 `AppSettingsRepository` 的 DataStore `ui_style` 键。
- `AppSettingsModel.uiStyle` 是唯一风格偏好。应用通过同一个设置 Flow 收集偏好，传给应用级主题；不要求调用 Activity 重启。默认 Material 3。
- 浅色、深色、跟随系统仍由独立 `themeMode` 决定。MIUIX 和 COUI 随明暗模式切换，但当前使用各自默认配色，没有映射 Material 动态取色和自定义主色。
- 主题结构固定为 MaterialTheme → MiuixTheme → CouiTheme，通过 LocalUiStyle 选择共享组件实现；不使用 `key(uiStyle)` 重建业务内容。
- 源码支持在同一页面树内重组时保留导航、Pager、表单及业务状态。没有设备测试证据证明实际动画、焦点、IME 或进程重启行为。

证据位置（均相对仓库根目录）：

- `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/settings/themesettings/ThemeSettingsScreen.kt:184`
- `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/settings/AppSettingsViewModel.kt:182`
- `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/data/repository/AppSettingsRepository.kt:180`
- `shared/src/androidMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/theme/Theme.android.kt:80`
- `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/App.kt:63`

## 页面与组件覆盖

| 范围 | 实际接入 | 限制 |
|---|---|---|
| 首页/校园门户 | 账号卡片、服务网格、网页入口、分组文字 | 校方网页保持原有 UI |
| 一级导航 | 三种风格的图标、文字、选中背景；底栏/侧栏适配 | 导航外壳仍复用 Material Surface/NavigationRail |
| 今日课表 | 课程卡片、文字、空状态、加载颜色、时间状态 | 课程业务数据共用；设备显示待测 |
| 周课表 | 课程文字、顶部周次入口、周次选择、课表选择、详情面板 | 课程色、尺寸、网格保留用户课表配置，不套三份原生卡片 |
| 设置主页 | 卡片、设置行、开关、周次/数字确认弹窗、配置入口 | 日期选择和滚轮保留通用控件 |
| 主题/语言/更多选项 | 风格、明暗模式选择、设置入口、更新渠道/结果弹窗、开发者开关 | 颜色选择器保留现有实现 |
| 通知设置 | 卡片、开关、选项、提前提醒输入、权限引导 | Android 系统权限页不换肤 |
| XATU 账号 | 学号输入、登录/切换/取消/退出按钮、操作反馈 | 统一认证验证码等由学校网页提供 |
| 成绩/考试/学业进度 | 共享卡片和文字、学期选择、加载/空数据/失败状态 | 无真实账号数据验收 |
| 校历 | 刷新、原图、OCR 文本/复制、网页入口、状态 | 原图/网页内容不重绘 |
| 电费 | 四级选择、余额、记录、趋势、刷新反馈 | 无校园网实测 |
| 签到位置 | 坐标输入、保存/启用状态、签到网页入口 | 权限及真实签到待测 |

未整体迁移的页面：课表管理、课程名称/实例管理、课程编辑、作息管理与编辑、课表个性化与小组件配置、假期管理、调课/快速删除、课表转换、备份恢复、适配仓库配置、贡献者/许可证展示和适配器流程。它们仍主要使用 Material UI；其中个别复用的课表组件会跟随风格，不能算完整迁移。WebView 原生工具栏、地址栏、菜单也仍使用 Material。

共享组件真实实现：

- 文字：Material Text、MIUIX Text；COUI 字体样式结合 BasicText，以保留课表字号、行数和对齐配置。
- 卡片：Material Card、MIUIX Card、COUI Group。
- 按钮：Material TextButton/Button、MIUIX Button；COUI 使用共享 Compose 按钮，映射其配色和圆角，不再以 Item 充当按钮。
- 输入框/开关：Material 和 MIUIX 使用各自组件；COUI 输入框使用共享 Compose 控件，开关使用 COUI Switch。
- 选择项：共享 RadioButton 语义、48dp 最小高度和选中背景。
- 弹窗/底部面板：三种风格共用 Material AlertDialog/ModalBottomSheet，颜色和圆角随风格变化，并非三库原生弹窗。
- 设置行：共用布局和回调，内部文字与颜色随风格变化。
- COUI 限制说明集中在外观设置，避免在每个业务页重复；上述共享控件不称为 COUI 原生。

## 状态、适配与主要路径检查

- 已按源码检查：门户 → 账号登录/切换/退出；门户 → 各原生校园服务；门户 → 学校网页；今日/周课表 → 课表选择、周次选择及课程详情；设置 → 主题、语言、通知及其他既有配置入口。
- 加载、空数据、失败、成功、保存中沿用原状态和回调，没有因风格分支复制网络或保存逻辑。服务反馈仍依赖现有状态字符串；未做真实接口失败注入。
- 门户网格结合可用宽度与 fontScale 计算列数；账号操作使用 FlowRow；导航空间不足时隐藏文字并保留图标名称与 Tab 选中语义。
- 周次选择使用自适应网格，今日课表保留课程状态语义。底栏和课表/设置有系统 Insets 处理。
- 这些是源码证据，不是窄屏、宽屏、横屏、200% 字号、键盘、安全区域或 TalkBack 设备验收。开关名称关联、长文本截断、拖动课表的无障碍操作仍需设备检查。

## 已确认的问题与待验证风险

1. **一级导航清栈影响完整换肤路径（优先）**：`App.kt:95` 的主页面跳转调用 `backStack.clear()`；`WeeklyScheduleViewModel.kt:56` 的浏览日期是 ViewModel 内存状态，周课表 `WeeklyScheduleScreen.kt:112` 从当前周对应的 Pager 中心初始化。换肤回调本身不清栈，但“浏览其他周 → 设置换肤 → 返回周课表”存在重新初始化周次/滚动位置的路径。清栈代码已确认，具体设备表现待复现；不能将它描述为全流程状态保留已通过。
2. **迁移覆盖未完成**：上表列出的深层页与 WebView 原生外壳仍保留 Material。只可认定核心页面接入共享组件。
3. **主色设置语义不完整**：`Theme.android.kt:80` 为 MIUIX/COUI 创建默认主题，主题设置仍显示动态取色/自定义主色；这些选项尚未覆盖两套库的原生颜色。
4. **测试边界**：现有两项风格测试只验证默认值和 COUI 已存偏好的解码；未验证三种风格的真实 DataStore 写入再开启、App 重启、运行时重组、导航状态和 WebView 会话。
5. **依赖版本与声明不同**：运行时依赖报告确认 Compose runtime/foundation 为 1.12.0，JetBrains Material 3 被从声明的 1.9.0 提升为 1.12.0-alpha03，AndroidX Material 3 为 1.5.0-alpha22。构建已通过，alpha 版本的设备兼容性仍需回归。
6. **许可资源版本信息落后**：Release APK 中当前资源路径的 aboutlibraries.json 包含 COUI/MPL-2.0 和 MIUIX/Apache-2.0，但 Material 3 元数据仍标为 1.9.0；APK 还包含旧 `xatuone.shared.generated.resources` 路径的另一份许可 JSON。应在后续发布前核对资源生成与实际解析版本；本轮未清理缓存或改生成文件。

## WebView 会话

- `WebView.android.kt:152` 通过 remember 取得共享 AndroidWebViewController；AndroidView factory 优先复用已有 WebView。
- `WebView.android.kt:397` 离开页面时从父容器脱离并 flush Cookie，没有 destroy 或换肤清 Cookie。
- 清 Cookie 的调用来自显式退出/账号切换等操作；风格偏好更新没有调用这些操作。
- 因此源码未发现换肤直接清除会话的逻辑。登录服务端过期、进程重启后的 Cookie 恢复、连续切换服务、验证码和跨域登录仍未实测。

## 依赖与许可证

- Android minSdk 为 31（Android 12+），Kotlin 插件声明 2.4.0、Compose 插件声明 1.11.1。
- MIUIX 为 `top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`，许可元数据为 Apache-2.0。
- COUI 为 Maven AAR `io.github.itoseo:coui:0.0.1-alpha7`，只放在 androidMain；`shared/build.gradle.kts:98` 明确关闭传递依赖，由宿主已有 Compose 依赖满足使用需求。未引入 `coui-material3` 桥接。
- 已核对当前 AAR 的 Maven POM、上游 LICENSE 和 APK 中的许可元数据：COUI 自有代码使用 MPL-2.0。第三方与名称说明以其 NOTICE/TRADEMARKS 为准；本记录不把构建成功等同于全部发行许可事项审查完成。
- 当前已用到的 COUI Theme、Group、Item、文字样式通过编译与 R8 构建。其余组件、可选视觉效果及全部设备 API 兼容性不在已验证范围。

## 验证结果

| 命令/检查 | 结果与执行方式 |
|---|---|
| `:androidApp:compileDebugKotlin` | 复用上一轮最终源码输入的通过记录，本轮未重跑 |
| `:shared:testAndroidHostTest` | 复用上一轮 16 项通过记录：风格偏好 2、课表逻辑 4、XATU 逻辑 10；0 failure、0 error |
| `:androidApp:lintDebug` | 复用上一轮通过报告，329 warnings、0 errors |
| `:androidApp:lintRelease :androidApp:assembleRelease` | 本轮执行，通过；R8 和资源压缩开启；Release lint 为 329 warnings、0 errors |
| `:shared:dependencies --configuration androidRuntimeClasspath` | 本轮执行，通过，确认真实解析版本及 COUI 的非传递依赖方式 |
| Release APK ZIP 检查 | 本轮执行，确认三种 ABI APK 生成及许可资源内容；没有安装 |
| Git 最终差异和 `git diff --check` | 本轮检查；已有修改保留，无平台外源码改动或新增生成文件改动 |
| `adb devices -l` | 无连接设备；没有设备、截图、账号或校园网流程验收 |

Release APK 为本地验证构建产物，不代表正式签名发布包。未新增或宣称执行不存在的 Compose UI、instrumentation 或端到端测试。

建议后续顺序：先解决一级导航返回状态保留和两套主题主色设置语义，再逐步迁移剩余页面、核对依赖/许可资源生成，最后以真实设备完成三风格 × 明暗模式、字号和校园服务的验收矩阵。

## 前后端功能一致性复查与修正（2026-11-07）

本轮保留全部已有修改，没有提交、推送或安装。检查当前源文件；代码索引早于已有改动，未将索引的无结果作为完整性证据。

| 检查项 | 三种风格共用的实际链路 | 结论与验证边界 |
|---|---|---|
| 首页及一级导航 | `XatuLoginScreen.android.kt` 的同一服务列表、`NavigationComponents.kt` 的同一目的地列表 | 未发现按风格隐藏入口；组件接收相同标题、副标题与回调 |
| 今日/周课表 | `TodayScheduleViewModel` / `WeeklyScheduleViewModel` → 同一课程模型 → 同一页面 | 名称（含脚本附加的课程序号）、教师、地点、周次、节次、学分及备注的显示条件不按风格分支 |
| 成绩、考试、学业进度 | `XatuAcademicRepository` / `XatuAcademicViewModel` → `XatuServicePage.android.kt` | 共用学期选择、成绩构成、汇总、考场/座位、学分及培养方案字段；风格不参与服务请求 |
| 校历、电费、签到 | 同一 Campus/Calendar Service、Campus Repository 快照和服务页面 | 原图/OCR、余额历史、坐标输入/保存/网页入口相同；没有风格专用假数据 |
| 加载、空数据、失败、成功、保存中 | 页面状态和 `StatusFeedback`，按钮统一接收 `enabled` / `onClick` | 数据含义及操作回调相同；反馈动画、字体、颜色可不同；未做设备失败注入 |
| 登录、切换、退出、WebView | `XatuSessionRepository`、共享 `AndroidWebViewController` | 风格更新不调用登录、清理或加载 URL；会话操作没有风格分支；真实认证会话未实测 |

### 本轮修正

- `App.kt` / `Navigation.kt`：原先一级导航会 `clear()` 全栈。现在只移除二级入口并将目标一级入口移至顶部，保留其他一级入口的键，供现有 Navigation 3 状态与 ViewModel 装饰器继续管理；重复点击不产生重复键。系统返回仍按栈返回。
- `UiStyleComponents.android.kt`：`UiCard` 原先在三套组件分支中分别组合内容。现在通过 `movableContentOf` 在外壳之间移动同一内容子树，避免换肤时重建卡片内的展开等状态；最新内容仍通过 `rememberUpdatedState` 更新。
- `AppSettingsRepository.kt` / `AppSettingsViewModel.kt`：风格操作只原子写入 `ui_style`，不再回写整个页面设置快照。存储键与格式不变；没有复制业务页面、Repository 或状态流。
- 先前阶段已将 MIUIX/COUI 不支持的主色控件改为明确说明；这是配色能力限制，不是校园业务功能差异。

### 最新验证

命令：`gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:lintRelease --offline --console=plain`。

- Android 编译通过；host tests **24 项通过，0 失败、0 跳过**。
- 本轮新增导航保留/去重测试 2 项，真实 Repository + DataStore 写入、关闭、重新打开测试 1 项（逐一覆盖 Material 3、MIUIX、COUI）。现有解码测试也覆盖三种风格。
- 持久化测试确认换肤不会修改所选课表 ID、明暗模式、提醒开关及提前时间，并确认不访问课程 DAO 或独立课表样式存储。
- Release lint 通过，329 warnings、0 errors。`git diff --check` 通过。
- 未运行 Compose UI / instrumentation 测试、没有安装或操作设备。本轮没有将卡片子树保留、Navigation 3 实际状态恢复、焦点/键盘、WebView 会话或真实校园服务认定为设备实测通过。
- 深层页及系统/学校网页的外观覆盖仍以原覆盖表为准；COUI 仍使用已明确说明的共享输入、开关与弹窗，未引入新的组件依赖。

## 本次运行时风格补齐（2026-11-07）

### 实现及共享边界

- 当前入口是「我的 → 界面与主题 → 界面风格」。Material 3、MIUIX、COUI 使用同一个 `AppSettingsModel.uiStyle`，只写入应用私有目录 `datastore/app_settings.preferences_pb` 的 `ui_style`。原有设置 Flow 驱动应用级主题即时重组，没有重启 Activity，也没有触碰课程、账号、缓存或 Cookie。
- `ShiguangScheduleTheme` → 固定的 MIUIX/COUI provider → Material 兼容主题 → 同一导航及页面。没有 `key(uiStyle)`，Repository、Service、模型、ViewModel、状态流、导航与业务规则没有按风格复制。
- 本轮补齐应用级中性色和圆角桥接：课程管理、编辑、备份、适配导入等仍使用原 Material 控件的页面，也能继承所选风格的视觉变量。保留课表自定义颜色和尺寸；不改变业务字段或课程规则。
- 三种风格统一保留浅色/深色/跟随系统、动态取色及自定义主色入口。MIUIX/COUI 的强调色同步现有主色；错误色语义不变。系统窗口底色使用适配后的主题色。
- COUI 开关使用发布 AAR 中确实存在的原生 `Switch`。COUI 服务块和服务行补齐与其他风格一致的最低高度（92dp、64dp）；MIUIX 服务行去掉独有的单行标题截断。选择项/按钮/导航继续共用至少 48dp 的点击目标及选中语义。
- 首页、校园门户、一级导航、今日/周课表、设置、XATU 账号及成绩/考试/学业/校历/电费/签到页面，继续使用同一页面和同一数据。源码检索未发现按风格分支的校园服务请求或业务字段过滤；课程详情仅随字体风格重新测量显示高度。
- 实际组件边界：MIUIX 使用原生文字/卡片/按钮/输入/开关；COUI 使用原生 Theme、Group、Item、开关和文字样式。COUI 通用卡片是共享 Compose Card 按其配色及圆角适配，输入、弹窗、日期及颜色选择器仍为共享 Compose 控件。未宣称全页面每个控件均为对应库原生实现。校方网页、Android 权限页、桌面小组件不随 Compose 风格切换。

### COUI 兼容性依据

核对官方仓库 `ItosEO/COUI-Kit` 的 README、构建文件、组件源码/文档及本地实际使用的 Maven POM/AAR，官方源码快照为 `520478b9305780b078bed1096ce68f727c594f14`。

- 技术栈为 Android Jetpack Compose，Android library 发布为 AAR，并非 commonMain 多平台库或 XML View 库。
- 官方当前源码使用 Kotlin 2.4.20、Compose BOM alpha2026.08.01；当前宿主为 Kotlin 2.4.0 / Compose 插件 1.11.1，实际 runtime/foundation 解析到 1.12.0。当前 alpha7 的已用 API 已通过该宿主编译、host tests 与 R8；这不代表任意未来版本或所有未使用组件兼容。
- Android minSdk 31；发布 AAR metadata 要求 minCompileSdk 37、minAgpVersion 9.1.0。当前工程 minSdk 31、compileSdk 37、AGP 9.3.1 满足要求。
- 发布坐标为 `io.github.itoseo:coui:0.0.1-alpha7`，官方 README 给出 Maven Central；构建亦配置本地 Maven 和 GitHub Packages。当前只依赖核心 `coui`，未使用可选的 `coui-material3`，未新增依赖。关闭传递依赖，复用宿主 Compose 和 AndroidX 能力。
- 可用组件目录包括 Scaffold、顶栏、搜索、Group/Item、导航、按钮/菜单、Slider、Checkbox/Radio/Switch、ChipTab、Dialog/InputDialog、日期/整数选择器等。目录存在不等于全部组件均已接入或设备验证。
- 自有代码许可为 MPL-2.0；分发需保留 LICENSE、NOTICE、TRADEMARKS 及第三方许可说明。MIUIX 0.9.4 为 Apache-2.0。本轮未修改第三方库源码，未把源码查阅等同完整发行合规审查。

官方依据：

- https://github.com/ItosEO/COUI-Kit/blob/520478b9305780b078bed1096ce68f727c594f14/README.md
- https://github.com/ItosEO/COUI-Kit/blob/520478b9305780b078bed1096ce68f727c594f14/coui/build.gradle.kts
- https://github.com/ItosEO/COUI-Kit/blob/520478b9305780b078bed1096ce68f727c594f14/LICENSE

### 实际验证结果

本轮一次执行：

```powershell
# 运行前配置本机的 JAVA_HOME、ANDROID_HOME 和 GRADLE_USER_HOME。
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:lintRelease :androidApp:assembleRelease --offline --console=plain
```

- BUILD SUCCESSFUL；Android 编译、Release lint、R8 和三种 ABI Release APK 构建通过。
- host tests：**50 项，0 失败、0 错误、0 跳过**。新增 2 项主题测试覆盖三风格 × 明暗模式的主色、错误语义保持，以及共享 Material 控件与库中性色的一致映射。
- 真实 DataStore 写入/关闭/重开测试逐一覆盖三种风格，确认其他偏好未改变，且风格写入不调用课程 DAO。现有导航去重/保留、课程和校园仓库测试通过。
- Release lint：**328 warnings，0 errors**；未顺带清理既有警告。
- `git diff --check` 通过；审阅主题、组件、测试与文档差异，没有覆盖前轮业务/品牌改动，没有提交、推送、发布或安装。
- **尚未验证**：三风格实机截图、窄屏/宽屏/大字号、输入焦点/键盘/拖动动画、实际导航状态和 WebView 登录会话保留、真实账号与校园网服务。构建和源码一致性不能替代这些验收。


## 原生组件与页面组织重构（2026-11-07）

本节覆盖此前“主要是 Material 换肤”的实现描述。原库顶栏、导航、排版、列表、选择和弹窗已经按风格分派；门户与首页允许不同布局，入口和业务状态仍来自同一份清单与 ViewModel。实现、平台边界及尚未迁移的专用控件见 `architecture.md` 的「原生风格页面组织」。

COUI 以项目实际 `0.0.1-alpha7` AAR 和对应 sources.jar 为依据。公开 API 与 JVM 方法导出不同：内部 `DialogButton` 和 `CouiDialogWindow` 不作为宿主接口调用。普通动作使用公开 Item，确认窗口使用公开 Dialog。API 34 浮动导航和 API 31–33 的原生行导航分别处理，大字号保留可滚动完整标签。

模拟器打开 COUI 周次窗口时，实际发现 `DialogProperties` 构造方法的 `NoSuchMethodError`。alpha7 发布 POM 的 BOM 为 `androidx.compose:compose-bom-alpha:2026.08.01`；本轮只据此对齐 Android Compose UI/动画/Foundation到 1.13.0-alpha02，保留既有 Material3 版本；未整体导入 BOM 的 Material3 等约束，不是用 Material 窗口冒充 COUI。未增加新的 UI 组件库。

页面正文保持在固定 Scaffold 槽位，仅分派可视槽位组件；没有把 NavDisplay 在不同库的 SubcomposeLayout 之间搬移。模拟器发现并修正了嵌套 movable-content 的运行时崩溃，以及 FlowRow 对原生选择行的内在尺寸测量冲突。MIUIX 紧凑顶栏初始折叠范围设为零，大标题由原库测量设置范围，避免吞掉正文滚动。

### 本轮最终验证

```text
./gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:lintDebug :androidApp:assembleRelease --offline --console=plain
```

- 最后一次构建 BUILD SUCCESSFUL（3m43s）；63 项 host tests，0 失败、0 错误、0 跳过。Debug lint 310 warnings、0 errors；Release 含 R8/资源压缩及 lintVital，通过。未针对已有警告做无关清理。
- API 34 雷电模拟器覆盖安装 Debug（保留数据）：三风格在外观页即时切换，切换后仍在外观页；账号与既有课表仍可查看。首页、教务目录、设置和课表可通过各库导航打开。
- 三库周次面板均实际打开；COUI 10→11→10 周切换成功。三库日期窗口均实际打开，COUI 选择 11 月 8 日并确认后，页面显示所选日期；随后恢复 11 月 7 日。
- MIUIX 紧凑标题页恢复滚动；周次通过明确的原生图标按钮打开，日期使用年/月/日 NumberPicker。COUI 日期窗口与周次 Dialog 的上述 ABI 崩溃已复测消失。
- COUI 检查 1.5 倍字号的账号信息及可横向滚动导航；横屏、深色检查设置与账号，矮屏顶栏改用紧凑形式。没有把这些抽查扩大表述为全页面、全部字号或全部 Android 版本验收。
- 测试结束恢复 Material 3、1.0 倍字号、竖屏和原浅色系统设置，回到原第 10 周周课表。未退出账号、未清缓存、未卸载应用、未操作实体设备。
- 本轮没有执行真实教务导入、真实校园网查询、登录验证码、跨账号切换、定位授权或 Cookie 跨重启认证流程。页面显示既有账号/缓存，不等于这些业务实时验收通过。API 31–33 导航分支及完整 TalkBack 操作尚未设备实测。
- `git diff --check` 通过。保留开始前工作区改动，不提交、不推送、不发布。以前章节的设备/网络结果仅对应各自旧构建，不替代本轮边界。



## Material 3 页面与交互重构（2026-11-07）

本轮只调整展示层及其布局。保留已有工作区修改；未变更依赖版本、业务规则、数据模型、请求和持久化。

实际 Android 运行依赖由 `:shared:dependencyInsight --dependency androidx.compose.material3:material3 --configuration androidRuntimeClasspath --offline` 确认为 `androidx.compose.material3:material3:1.5.0-alpha22`。版本目录的声明值与最终解析值不同，本轮未升级依赖。

### 修改范围

- `ui/components/UiNativeLayouts.android.kt`：Material 原生 LargeTopAppBar、滚动折叠；保留固定 Scaffold 正文槽位。紧凑页的初始折叠范围为零。
- `ui/settings/xatu/XatuHomeScreen.android.kt`：课程主摘要采用 ElevatedCard；主操作与次操作采用 FilledTonalButton、TextButton。
- `ui/schedule/WeeklyScheduleScreen.kt`：SingleChoiceSegmentedButtonRow / SegmentedButton 切换日周视图；Button 同步、OutlinedButton 设置。宽屏单行、窄屏分行，不调整字号。
- `ui/settings/xatu/XatuPortalScreens.android.kt`：Material 教务、校园服务采用 ListItem、图标和 HorizontalDivider，保留同一入口清单与搜索状态。
- `ui/settings/xatu/XatuServicePage.android.kt`：成绩、考试学期使用 ExposedDropdownMenuBox、OutlinedTextField、DropdownMenuItem；回调与原选择逻辑相同。
- `ui/settings/xatu/XatuElectricityPage.android.kt`：刷新主操作、切换房间次操作、ListItem 最近记录、ModalBottomSheet 完整记录、四级宿舍下拉选择。
- `ui/settings/xatu/XatuLoginScreen.android.kt`：账号与登录状态使用 ListItem；登录主按钮和进行中指示。
- `ui/settings/SettingsHomeScreen.kt`、`ui/settings/CourseToolsScreen.kt`：分组标题、ListItem 与分割线，课程配置和应用设置保持各自归属。

以上路径均位于 `shared/src/{androidMain,commonMain}/kotlin/com/xingheyuzhuan/shiguangschedule/` 下。原有 Material NavigationBar、AlertDialog、DatePickerDialog/DatePicker、周次 ModalBottomSheet、SnackbarHost 和进度控件继续使用，未用自绘形状冒充组件。

### 自动化验证

```text
./gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:lintDebug --offline --console=plain
```

最后一次构建 BUILD SUCCESSFUL（1m42s）；63 项 host tests，0 失败、0 错误、0 跳过。Debug lint 310 warnings、0 errors。未做无关警告清理。本轮未重新执行 Release 构建。

### 模拟器验证

API 34 雷电模拟器、360dp 窄屏，覆盖安装 Debug 保留数据。检查了首页及滚动折叠、五个一级导航、教务入口、日周切换、10→11→10 周切换、日期窗口打开与取消、课程同步入口到适配器和 WebView、学期下拉选择及历史成绩汇总、电量刷新与更新时间、房间四级下拉恢复、完整历史底部面板、账号输入展开与取消、退出确认与取消、设置分组及外观切换。

默认字体与 1.5 倍字体检查账号和课程；640dp 横屏检查首页和课程。横屏课程操作区由两行压成单行，保留字体与全部操作。深色模式抽查首页、课程；Material 3→MIUIX→Material 3 切换停留外观页，恢复后既有课表和账号仍在。初次首页滚动截图中折叠标题处于异常过渡帧，之后展开、折叠及离开返回多次复查稳定态显示正常，未为此加入兼容分支。

截图存于本机验收目录。测试结束恢复 Material 3、1.0 倍字体、竖屏和浅色系统模式。未确认退出、未修改开学日期、未切换或保存其他宿舍，未清除用户数据。

边界：模拟器界面和操作检查不等于所有业务验收。未重新执行完整课程导入、验证码登录、账号切换/退出后重新认证、Cookie 跨重启恢复、校园网限定服务及实体设备/TalkBack 全流程。成绩和电费的显示与刷新操作已检查，不把已有缓存展示当作实时接口验收。

## MIUIX 页面与交互重构（2026-11-07）

本轮在既有工作区继续修改展示层，没有提交、推送、修改依赖或变更业务模型、接口、请求、存储与 WebView 会话管理。

### 版本与实际组件

`:shared:dependencyInsight --dependency miuix-ui-android --configuration androidRuntimeClasspath --offline` 确认 Android 实际依赖为 `top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`。API 以该版本本地 sources.jar 和编译结果核对，未引入额外 UI 库。

- 首页：原生 TopAppBar、SmallTitle、Card、BasicComponent、TextButton；账号与连接状态单列呈现，课程摘要前置，常用服务双列排列，同步使用主按钮样式。
- 课程：TabRowWithContour 切换日/周视图；保留同一 dayMode、周次、课表与操作回调。宽屏将切换与操作并排，窄屏分行。选择周次使用原生 IconButton / WindowBottomSheet，日期继续使用 WindowDialog / NumberPicker。Snackbar 接入库中 Snackbar，但沿用现有 SnackbarHost 状态、超时与撤销回调。
- 教务：BasicComponent 呈现刷新及更新时间、成绩、考试和学业课程。成绩行可展开原有成绩构成；原始成绩、学分、绩点、代码、学期及考试字段均保留。学期采用 WindowListPopup / DropdownImpl / DropdownItem，当前值使用 DropdownArrowEndAction。
- 电费：SmallTitle + Card 分组，刷新与切换房间为 BasicComponent 动作行，进行中使用 CircularProgressIndicator；历史使用 WindowBottomSheet；四级房间选项使用同一原生列表弹层。列表内容使用 LazyColumn，打开时定位当前选项；请求和保存仍由原有仓库执行。
- 账号：BasicComponent 的正文槽位纵向排列完整学号与状态；TextField 编辑，TextButton 主操作，重新登录为原生动作行；退出沿用 WindowDialog 并在验证时取消。
- 设置：SmallTitle + Card + BasicComponent 分组，应用设置前置、业务偏好收纳；日期与周次页改用原生顶栏。既有原生 RadioButton、Switch、NumberPicker 继续使用。
- 主导航继续调用原生 NavigationBar / NavigationRail；正文 Scaffold 组合槽位不变，不因换肤搬移 NavDisplay 或 AndroidView。共用文字样式沿用应用级 MiuixTheme.textStyles 映射，避免再次映射而改变既有文字尺寸。

### 修改文件

以下均位于 `shared/src/{androidMain,commonMain}/kotlin/com/xingheyuzhuan/shiguangschedule/`：

- `ui/components/UiNativeLayouts.kt`、`UiNativeLayouts.android.kt`、`UiStyleComponents.android.kt`
- 新增 `ui/components/MiuixValuePicker.android.kt`（学期和电费选项共用）
- `ui/schedule/WeeklyScheduleScreen.kt`
- `ui/settings/SettingsHomeScreen.kt`、`CourseToolsScreen.kt`、`SettingsScreen.kt`
- `ui/settings/xatu/XatuHomeScreen.android.kt`、`XatuPortalScreens.android.kt`、`XatuServicePage.android.kt`、`XatuElectricityPage.android.kt`、`XatuLoginScreen.android.kt`

### 验证

```text
./gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:lintDebug --offline --console=plain
```

最终构建 BUILD SUCCESSFUL（31s）。63 项 host tests，0 失败、0 错误、0 跳过；Debug lint 310 warnings、0 errors。本轮没有执行 Release 构建，未清理无关既有警告。

API 34 雷电模拟器覆盖安装 Debug、保留数据。实际检查首页、目录和设置导航，日/周切换、10→11→10 周、原生周次窗口、日期窗口打开取消、历史学期选择、历史成绩汇总与成绩构成展开、刷新电量后的更新时间变化、房间选项菜单、完整历史底部面板及按日/按次切换、账号编辑与取消、退出窗口与取消。默认字体、1.5 倍字体检查账号；640dp 横屏检查课程操作区，并检查横屏大字号账号；深色抽查首页。Material 3→MIUIX 即时切换后仍停留外观页，返回课程页仍保留第 11 周；随后恢复第 10 周。最终为 MIUIX、1.0 倍字体、竖屏、浅色；没有确认退出、改变开学日期或保存其他宿舍。

截图记录位于本机验收目录。本轮检查范围未见 AndroidRuntime 崩溃。

边界：成绩、电量显示和刷新操作不等于全部接口、校园网及完整业务实测。没有重新执行验证码登录、完整课程导入、跨账号切换、主动退出后重新认证、Cookie 跨重启恢复、定位授权、实体设备与 TalkBack 全流程；课程 Snackbar 的回调适配通过编译，未为触发撤销通知而改动真实课程。没有把校园网访问限制作为本轮 UI 缺陷处理。

## COUI 页面与交互重构（2026-11-07）

### 版本与兼容边界

- 实际解析 `io.github.itoseo:coui:0.0.1-alpha7`（dependencyInsight，离线），按该版本 sources.jar 核对公开 API。没有升级依赖。
- 官方 README 和 LICENSE 确认核心为 Android Jetpack Compose、MPL-2.0；发行须保留 NOTICE 等第三方声明。本轮未修改库源码。官方 main 的构建属性为 `0.1.0-SNAPSHOT`，不将其称为稳定发布。
- 工程 Kotlin 2.4.0、Compose 插件 1.11.1，Android Compose UI/foundation/animation 对齐 1.13.0-alpha02。与当前 alpha7 的兼容结论来自本工程编译及 API 34 模拟器运行，不外推到任意 Compose 版本。
- ChipTabRow、浮动导航和 TopAppBarButton 要求 API 34。API 31–33 保留原生 Item/RadioButton 方案；本轮未在 API 31–33 设备运行。

### 实际组件与页面变化

- 首页：LargeTopAppBar、Group、Item、Text、NavigationBar。账号与连接状态单独纵向呈现；常用操作采用原生列表，课程/电量/考试沿用同一状态来源。原生分组自行管理外边距，移除重复页面内缩。
- 课程：ChipTabRow 切换同一个 dayMode；TopAppBarButton 提供返回/课表选择操作。宽屏将视图切换和操作并排，窄屏分行。已有 Dialog、CalendarPicker、WheelPicker、RadioButton 继续提供选择操作；不变更课程网格、周次、详情及导入链路。
- 教务：Group、Item、Menu/CouiMenuItem/CouiMenuAnchor；成绩列表点击展开真实成绩构成，保留学期、代码、学分、成绩、绩点；考试字段不变。刷新动作用原生列表项呈现更新时间和进行中状态。
- 电费：原生 Group/Item 组织宿舍、刷新、切换房间和历史记录；四级选项复用原生 Menu；完整记录和按日汇总使用原生 Dialog。保留实际历史图表和“度”单位；修正原生分组内部文字贴边。
- 账号：原生 Item 纵向显示完整学号和登录状态，InputDialog 编辑学号草稿，Dialog 确认退出；验证时仅取消，不退出、不修改账号。
- 设置：Group 原生分组标题，应用与账号、学习与校园分别收纳；原生 Item、Switch、RadioButton、Dialog 沿用同一偏好来源。

### 公开组件缺口与本地实现

alpha7 没有公开通用 Button、Snackbar 或 indeterminate progress API。DialogButton 为 internal；TopAppBarButton 必须在顶栏作用域使用，不能拿来当正文按钮。因此：

- `CouiAction.android.kt`：Foundation + CouiPageDefaults.shape、Text 和公开 couiButtonFeedback 的最小正文按钮。
- `CouiLoadingIndicator.android.kt`：Foundation Canvas 旋转弧线及 ProgressBarRangeInfo，无 Material 进度条。
- `CouiCourseSnackbar`：原生 Group/Item 与本地动作按钮承载内容；超时、关闭、撤销仍由原 SnackbarHost 合同控制。未改业务事件。
- 课程网格和电量趋势是原有业务可视化，库没有对应现成组件，不冒充 COUI 原生组件。
- 根 Scaffold 保持固定正文组合槽位，避免换肤搬移 NavDisplay/AndroidView；顶栏、导航和页面内容实际采用 COUI。官方允许使用宿主容器。

### 本轮源码范围

均在 `shared/src/{androidMain,commonMain}/kotlin/com/xingheyuzhuan/shiguangschedule/`：

- 新增 `ui/components/CouiValuePicker.android.kt`、`CouiAction.android.kt`、`CouiLoadingIndicator.android.kt`。
- 修改 `ui/components/UiNativeLayouts.kt`、`UiNativeLayouts.android.kt`、`UiStyleComponents.android.kt`、`UiFormComponents.android.kt`。
- 修改 `ui/schedule/WeeklyScheduleScreen.kt`、`ui/settings/SettingsHomeScreen.kt`。
- 修改 `ui/settings/xatu/XatuHomeScreen.android.kt`、`XatuPortalScreens.android.kt`、`XatuServicePage.android.kt`、`XatuElectricityPage.android.kt`、`XatuLoginScreen.android.kt`。
- 不修改 Service、Repository、业务模型、请求、存储格式、WebView/Cookie、依赖或包名；保护开始时已有 176 项工作区修改/未跟踪文件，不提交、不推送。

### 验证记录

命令：`./gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:lintDebug --offline --console=plain`。

63 项 host tests，0 失败、0 错误；Debug lint 0 errors、310 warnings。根据实际截图修正内边距、账号分组标题与宽屏课程操作区后重跑受影响任务；最终日志仅保存在本机。`git diff --check` 通过。没有运行本轮 Release 构建。

API 34 雷电模拟器保留数据覆盖安装。已操作日/周切换、教务目录、学期原生菜单、全部历史成绩和成绩构成展开、电费刷新、房间选项菜单、完整历史及按日/按次切换、学号输入弹窗和取消、退出确认和取消、MIUIX↔COUI 即时切换。电费刷新后，页面显示的剩余电量和更新时间均有变化，不将刷新前的缓存当作实时请求成功。恢复后账号仍在、课表仍为第 10 周。默认及 1.5 倍字体检查账号，640dp 横屏检查账号/课程，深色抽查首页/课程。截图位于本机验收目录。

边界：本轮不重新执行验证码登录、完整课程导入、跨账号切换、主动退出再登录、Cookie 跨冷启动恢复、定位授权、校园网限定校历和实体设备/TalkBack 全流程。课程 Snackbar 回调通过编译，未为触发撤销而改真实课程。未把页面打开、已有成绩展示或构建成功表述为全部业务实测。最终恢复 COUI、正常字号、竖屏、浅色；未清数据、未更改开学日期或保存其他宿舍。

最终受影响任务 BUILD SUCCESSFUL（1m18s）；复查课表原生选择窗口并取消、1.5 倍字号课程及640dp宽屏课程单行操作区。恢复默认显示条件，保留第10周；最终 AndroidRuntime 日志检查范围未见 com.xatuone 的 FATAL EXCEPTION。未确认退出、未保存账号或宿舍变更。
