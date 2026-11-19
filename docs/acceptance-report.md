# 一校通功能验收记录

> 最新结果见文末「当前工作区前后端一致性验收」。此前各轮结果保留为历史证据，不能替代当前 APK 的实机验收；尤其不能把旧模拟器安装包当作本轮源码的运行结果。

验收日期：2026-11-07

验收对象：当前工作区 `main` 分支的一校通 Android 实现。工作区改动保留，未提交或推送。

## 结果摘要

**最新结论：本次要求的四项问题已修复并在模拟器复验；完整功能验收仍受真实导入、校园网和实体设备边界限制。** 四项复验见下一节。后文完整链路回归保留为修复前的历史证据。校历按用户确认属于校园网环境限制，本轮未修改校历请求或解析。

## 四项问题修复复验（2026-11-07）

| 问题 | 修复 | 验证与边界 |
|---|---|---|
| 同步课表暂无适配器 | 内置仓库加入现有 XATU 脚本和索引；资源初始化补齐缺失文件及索引条目，保留已下载脚本、索引版本和其他学校；学校查询等待初始化完成 | Host test 验证升级合并、重复运行不重复条目且不覆盖下载脚本。覆盖安装后出现 XATU 适配器，私有目录有脚本；实际执行到学期确认且默认勾选当前秋季学期，随后取消，未覆盖现有课表；完整写入/课程字段仍待专用课表验收。 |
| 平均绩点误为年份 | 按真实学期统计表的“在校汇总”行取课程数、总学分和平均绩点；文本标签解析排除表格，避免跨表头读取年份 | 已核对历史成绩页，课程数、总学分和平均绩点均与教务响应的汇总行一致；新增总计行及无总计行测试，原有文本汇总样本继续通过。 |
| 默认未来学期 | 使用当前教学周和北京时间反推第一教学周周一，再匹配相应学年/学期；不再默认列表首项；仍允许手动选择 | 成绩与考试首次进入均选 2026-2027学年1。测试覆盖未来学期排第一、春季、跨年秋季及当前学期缺失不误选未来。读取周次失败时保留学期选项并呈现错误，不静默选择无关学期。 |
| 系统返回跳到“我的” | 门户内部服务子页接管 Android BackHandler，与工具栏统一清除当前子页 | 模拟器成绩页系统返回、考试页工具栏返回均回门户；不修改 WebView 自身后退/会话逻辑。 |

改动范围：`shared/assets/offline_repo/`、`ResourceInitializerManager.kt`、`SchoolRepository.kt`、`XatuAcademicModels.kt`、`XatuAcademicRepository.kt`、`XatuAcademicViewModel.android.kt`、`XatuLoginScreen.android.kt` 和相关 host tests。沿用原模型、存储格式、桥接与导入脚本流程，没有改成绩接口或重写业务页面。

验证命令：

```text
gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:lintRelease --offline --console=plain
gradlew.bat :androidApp:assembleRelease --offline --console=plain
git diff --check
```

编译、Debug 构建和 **29 项 host tests（0 失败/错误/跳过）**通过；Release lint 为 0 errors、329 warnings。日志 `build/four-fixes-verification.log`；R8 Release 构建通过，用时 1m39s，见 `build/four-fixes-release.log`。模拟器覆盖安装保留原有数据与登录，未执行退出、充值或签到。截图/XML 前缀为 `fixed-`、`import-`，位于本地忽略目录 `build/acceptance-ui/`，不公开含个人信息的截图。

本轮未修改校历；没有将校园网访问失败当作接口缺陷。未提交、推送或发布。

## 前次检查记录（历史，不代表本次在线结果）

| 功能 | 结果 | 证据与限制 |
| --- | --- | --- |
| Android Release 与 R8 | 通过 | `:androidApp:assembleRelease` 成功，生成 armeabi-v7a、arm64-v8a、x86_64 APK。 |
| 教务适配解析 | 代码验证通过 | Android host tests 覆盖学期、成绩、考试、培养方案和课程目录解析；没有真实教务会话，未验证在线请求结果。 |
| 学期默认选择与切换 | 代码验证通过 | 页面按教务返回列表默认选第一学期，选择变化会取消旧请求；需登录设备验证服务端当前学期排序。 |
| 成绩、考试、学业进度 | 待账号实测 | service、repository、ViewModel 和页面入口均存在；需要 XATU 登录会话验证接口响应、空数据和错误反馈。 |
| 登录、Cookie 持久化、退出与切换账号 | 部分通过 | WebView 使用 Android 持久 Cookie 会话；账号切换已清理旧数据并等待 Cookie 删除完成后再导航。进程重启恢复、CAS 验证码和重新登录需设备实测。 |
| 校历和 OCR | 部分通过 | 校历首页可公开访问，已公开的 2025-2026 校历页面与原图返回 HTTP 200。最新 2026-2027 页面要求校园网权限，当前外网无法验证；OCR 需 Android ML Kit 设备运行验证。 |
| 电费余额与历史趋势 | 待校园服务账号实测 | 项目、校区、楼栋、房间选择、余额请求、历史记录和趋势图均已接入；需缴费平台登录 Cookie 和真实宿舍信息验证。 |
| 签到定位权限 | 待设备实测 | 仅 XATU 签到页面可调用地理位置桥接；清单含粗略/精确定位权限；需验证系统权限弹窗、拒绝/允许流程及签到页定位回调。 |
| 一站式大厅、请销假、图书馆、校园网、教务入口 | 待设备及账号实测 | 页面入口已配置；需要在设备上检查跳转、单点登录和各站点移动端显示。 |
| 首页功能入口 | 代码验证通过 | 同步课表、成绩、考试、电费、校历、学业进度、签到及校园服务入口均可到达对应导航目标；无设备无法点按验证。 |

## 前次自动化验证记录

```text
:shared:compileAndroidMain       通过
:shared:testAndroidHostTest     通过
:androidApp:lintRelease         通过
:androidApp:assembleRelease     通过
actionlint                      通过
git diff --check                通过
```

## 前次现场待办记录

连接一台 Android 设备并处于校园网环境后，依次完成：XATU 登录与验证码、完成登录、课程同步、成绩/考试/学业计划、最新校历原图与 OCR、电费项目选择和余额刷新、签到权限允许与拒绝、切换账号、App 重启恢复 Cookie、主动退出后重新登录。以上流程未在本次环境中伪造为通过。

## 修复前完整链路回归（2026-11-07，历史证据）

对象：`main`，基线 `48ed6cf` 加全部现有未提交改动。本轮只更新本报告，未修改正式 UI、业务、接口或依赖；保护所有原有修改，未提交、推送或发布。

为验收当前源码，已向雷电 `emulator-5554` **覆盖安装 Debug x86_64 APK**，没有卸载、清数据或执行退出登录。使用已有登录会话，未连接实体手机。截图和 UIAutomator XML 在忽略目录 `build/acceptance-ui/`；部分含个人信息，不应公开打包。

### 功能对齐表

表中源码基于 `shared/src`，Android 包路径为 `androidMain/kotlin/com/xingheyuzhuan/shiguangschedule/`，共享包路径对应 `commonMain`。

| 功能 | 请求 → 数据/状态 → UI → 操作/保存 | 本次结果及边界 |
|---|---|---|
| 课程同步 | SchoolRepository 索引 → AdapterSelectionScreen → WebView 脚本 → WebBridgeHandler → CourseConversionRepository → Room/配置 → Today/Weekly VM → 课表 | **当前环境阻断**。点击同步课表提示本科/专科类别暂无适配器；应用私有目录无 XATU 脚本。仓库离线资源只有示例脚本；未拿另一仓库旧脚本替代本安装证据。真实导入、保存、完成反馈未验收。 |
| 课程字段 | ImportCourseJsonModel → importCourses → 名称/教师/地点/星期/周次/节次/自定义时间/学分 → 卡片和详情 | 源码链路存在，渲染逻辑测试通过；当前模拟器没有已配置课程，长课程名、多教师、地点及真实详情未验证。桥接按既有约定清除导入的 color/remark，不能声称脚本备注被保留。 |
| 当前教学周 | homeExt!main.action → AcademicService.officialWeek → Repository → Academic VM → 官方教学周页 → 刷新 | **真实请求部分通过**：显示第 10 周；恢复 COUI、force-stop/start 后再次刷新仍返回第 10 周。未与学校人工校历独立核对。 |
| 开学日期 | 导入脚本 → 配置 JSON → importCourseConfig → semesterStartDate/semesterTotalWeeks → 课表日期/设置 | 原生周次页明确只查询、不回写开学日期。周课表显示“请设置开学日期”；当前缺适配脚本，反推日期及落盘完整链路未通过。 |
| 课程序号 | 无独立导入字段；脚本需拼入 name → 课程名显示 | 源码能传递名称，但未实际运行 XATU 适配脚本，无法确认正确序号。成绩页面的课程代码不等于课程表序号导入已通过。 |
| 成绩/学分/绩点 | person.action/search/historyCourseGrade → Academic Parser → XatuGradePage → 账号+学期加密缓存 → VM → GradesContent/GradeRow → 学期/刷新/构成 | **部分实测**：全部历史显示真实课程名称、代码、学分、成绩和单课绩点；平均绩点误显示为学年年份，汇总正确性不通过。当前学期“暂无成绩”不能证明服务器确实没有成绩。 |
| 考试 | stdExamTable/examTable → XatuExam → 缓存/VM → ExamsContent → 学期/刷新 | 日期时间合并、地点、座位、代码、考试名/备注的解析样本测试通过。模拟器默认未来学期显示暂无考试；没有真实非空样本，不认定在线字段正确。 |
| 学业进度/培养方案 | myPlanCompl + myPlan → XatuStudyPlan/catalog → Repository → VM → PlanContent/分类课程目录 → 刷新 | **部分实测通过**：已修/要求学分、专业、GPA、分类、课程完成情况出现真实数据；fixture 测试通过。未逐门与教务网页核对全部目录；报告不记录姓名和学号。 |
| 校历日期/原图/OCR | 公开索引 → 详情/图片 → ML Kit OCR → XatuCalendar → campus 快照 → 日期/原图查看器/文字 → 刷新/复制/来源 | **本次在线失败**：显示 Collection contains no element matching the predicate.，无成功快照；原图、缩放、OCR 和日期提取未验收。不能把校园网权限失败直接当作接口缺失。 |
| 电费余额/历史/刷新 | jfpay 项目/校区/楼栋/房间/余额 → CampusService → 查询状态 → saveDorm/saveBalance → 快照 → 余额/本机历史/趋势 | **部分实测**：项目弹窗返回两个真实选项。未杜撰宿舍或查询真实余额，余额单位待核对。宿舍历史隔离、过期宿舍结果拒绝落盘的测试通过。趋势是本机查询记录，非平台账单。 |
| 签到位置/网页 | 输入 → XatuLocation 校验 → saveCheckinLocation → 快照 → 页面/限签到路径 JS 桥接 → 网页 | 页面输入/保存/开关/网页入口已检查；当前输入保存逻辑及无效坐标拒绝测试通过。未改变用户坐标、启用模拟位置或提交签到；权限、回调及学校最终签到待实体设备。 |
| 登录/切换/退出 | WebView/CookieManager → 完成登录探测 → SessionRepository/账号偏好；清会话/缓存后切换或退出 | 已有会话可读取部分学术数据；未重新登录、切账号、退出，避免破坏现有认证。统一清理和等待顺序仅源码确认，真实隔离和重新登录待验收。 |
| WebView 会话 | shared controller → 页面移除父容器并 flush → 同一 CookieManager → 原生请求读取 Cookie | 换肤不调用会话清理；进程重启后教学周仍请求成功。未验证网页连续跳转、验证码、实例身份及跨站单点登录；Cookie 可用不能代替完整网页会话验收。 |
| 校园网页入口 | 门户标记网页服务 → Destination.WebView → 共享 controller | 源码入口存在；请销假、图书馆、网费、一站式等实际认证/权限未验收，不称原生业务已完成。 |
| 三风格一致性 | 单一 ui_style DataStore → 应用主题 → 相同页面/VM/Repository/回调 → 共享组件外观 | 模拟器即时切换三风格；Material/MIUIX 返回首页保留同一校历子页和错误含义；恢复 COUI 并进程重启保留风格。持久化/其他偏好不变/导航测试通过；不是所有业务×三风格的设备全矩阵通过。 |

### 已确认缺陷与证据

1. **P1 课程同步当前被适配器缺失阻断**：`commonMain/.../data/repository/SchoolRepository.kt:58,107` 读取内部索引并按学校 ID 取适配器；`ui/schoolselection/list/AdapterSelectionScreen.kt` 显示空结果。模拟器 `sync.xml/png` 已复现。先核对实际索引、XATU ID/分类、资源更新，再以专用课表真实导入。
2. **P1 历史平均绩点错误**：模拟器 `grades-history-real.xml/png` 显示的平均绩点为学年年份。`androidMain/.../xatu/academic/XatuAcademicModels.kt:179-184` 将整页 HTML 转文本再匹配标签后数字，`ui/settings/xatu/XatuServicePage.android.kt:509-524` 直接显示。错误结果已确认；误取年份符合解析方式，但本次未保存原始响应定位具体匹配片段。应限定真实汇总区域并补无汇总值、带表头和年份的样本。
3. **P2 默认未来学期**：成绩/考试首次选中 2027-2028学年1，当前日期是 2026-11-07。`XatuAcademicViewModel.android.kt:111-117` 默认列表首项，不能把最新可选学期视为当前。
4. **P2 校历失败且错误难理解**：`xatu/campus/XatuCalendarService.kt:21-36` 对链接/图片使用 first 匹配；服务页 `:76-89` 展示异常。未取得失败响应，无法确定权限页面还是结构变化；校园网限制本身不记为代码故障。门户“学期日期与假期”也超过现有结构化日期范围能力。
5. **P2 返回行为不同**：首页学术服务中，工具栏返回回门户，系统返回曾跳到之前“我的”；点击已选中的首页仍留在子页。`XatuLoginScreen.android.kt:91,99-109` 用局部 selectedPage 管子页，未接管系统返回；`App.kt` 弹全局栈。未通过同层返回一致性验收。

### 尚需进一步验证的源码风险

- `XatuAcademicService.kt:61` 优先取页面 semesterCalendar.value，可能覆盖用户选择的学期 ID；需比较多学期原始响应，目前未确认发生真实数据错配。
- `XatuAcademicService.kt:74-83` 返回首个非空考试批次，不合并后续批次；需真实多批次样本判断遗漏。
- 再次进入成绩页曾恢复默认未来学期，需核验局部服务组合销毁后的 ViewModel/选择状态所有权，纯导航键测试不能替代此流程。
- 成绩截图内容延伸到浮动导航后方；服务滚动容器缺少门户相同的底部留白。最后字段/按钮能否完全滚动露出需补测，不凭中间截图认定永久不可操作。

### 尚未形成完整闭环的能力

- 原生教学周一键写入课表日期（原生只查询，脚本本轮被适配器缺失阻断）。
- 独立课程序号字段或显示开关（现依赖拼接课程名）。
- 结构化假期及校历日期应用到课表配置。
- 平台电费历史账单/消费趋势（已有本机余额查询历史）。
- 原生签到提交及学校确认成功状态（当前通过网页完成）。

本轮不扩展这些功能，不以假数据补齐。

### 本轮验证命令与结果

本机通过 `JAVA_HOME` 和 `GRADLE_USER_HOME` 配置构建环境。

```text
gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:lintRelease :androidApp:assembleRelease --offline --console=plain
gradlew.bat :androidApp:assembleDebug --offline --console=plain
git diff --check
```

- 主检查 BUILD SUCCESSFUL，1m41s，日志 `build/full-alignment-acceptance.log`；Debug 构建成功，日志 `build/acceptance-debug-build.log`。
- **24 tests，0 failures，0 errors，0 skipped**，XML 在 `shared/build/test-results/testAndroidHostTest/`。包括解析样本、坐标/缓存、宿舍状态、课表渲染、导航及三风格真实 DataStore 重开。没有完整真实 API/Compose instrumentation/E2E 套件；GPA 缺陷说明现有样本不足。
- Release lint **0 errors，329 warnings**，报告 `androidApp/build/reports/lint-results-release.html`；没有无关清理警告，不表述为零警告。
- R8 和资源压缩执行成功，三个 ABI Release APK 已生成；部分 native 库无法 strip 而保留原库。不代表正式发布签名验收。
- diff 空白检查通过，LF/CRLF 提示不是空白错误。未对输入未变、已通过的 Gradle 检查反复运行。
- ADB UIAutomator + 截图为本次手动集成检查；没有伪称自动 UI 测试全覆盖。

### 分类结论

| 类别 | 范围 |
|---|---|
| 已通过源码和自动化验证 | 编译、24项测试、lint任务、R8构建、空白检查；三风格共用模型/Repository/业务回调。 |
| 模拟器真实账号已观察 | 单课历史成绩/学分/绩点、学业进度部分数据、教学周；也复现汇总错误与未来学期默认项；电费项目选项可读。 |
| 需要真实账号继续验证 | 非空考试、多批次、各学期成绩逐项对照、真实宿舍余额/单位/刷新、验证码/切换/退出/登录失效、适配器更新后真实导入。 |
| 需要校园网验证 | 最新校历来源/原图及OCR，受限教务、一站式和其他网页服务。外网失败不直接认定接口不可用。 |
| 需要实体设备验证 | GPS/权限、学校签到、厂商WebView验证码、后台恢复/手机重启、窄屏/大字号/横屏/无障碍及三风格完整业务矩阵。模拟器 force-stop 不等于上述验证。 |
| 剩余前后端不一致 | 同步入口缺适配器；成绩汇总错误；默认非当前学期；校历数据未取得；原生周次与课表日期未闭环。 |

下一轮优先级：索引/脚本主路径 → 真实成绩汇总解析与回归样本 → 当前学期默认项 → 校历原始响应核对 → 返回行为 → 账号/校园网/实体设备矩阵。

COUI 仍为 alpha7 部分原生组件及明确说明的共享 Compose 输入/开关/弹窗，不是全量原生覆盖；本轮未变依赖或重新作上游许可审计，已有依赖/许可结论见 `ui-style-regression-2026-11-07.md:78-85`。

## 当前工作区前后端一致性验收（2026-11-07）

### 对象、方法与结论边界

- 仓库 `.`，分支 `main`。保留所有已有未提交改动；本轮只更新本验收记录，没有改业务源码、依赖或资源，没有提交、推送、安装、清数据或执行课表覆盖写入。
- 从请求/网页、Service/Repository、模型、ViewModel/局部状态、页面、用户事件到保存/下一次请求逐项阅读。代码图索引早于当前部分文件，已对主要学术/校园/账号链路改用当前源码核查，没有将索引缺失当作功能缺失。
- 字段对齐结论限于当前源码已解析、建模的业务字段。本轮未抓取当前真实服务器响应，不能承诺服务端额外返回、尚未建模的字段均已覆盖。
- 查验已有 50 项 host test XML、上一轮当前源码的构建结果、实际 Release APK 的 metadata 与内置 ZIP；源码输入未变化，不重复运行上一轮已通过的 Gradle 检查。本轮另外执行 AAPT2、APK ZIP/脚本一致性检查、ADB 安装版本比对和 `git diff --check`。
- ADB 存在 `emulator-5554`，安装的 `com.xatuone` 是 versionCode 36 / 2.1.0，更新时间为 2026-11-07 13:56:01。其 base APK SHA-256 与当前工作区三个 Debug 包和 x86_64 Release 均不一致。本轮没有把该旧安装包的页面或账号状态作为当前实现的验收证据，也没有访问账号凭据。
- **总体结论：主要业务字段已有消费页面，但不能判定完整功能验收通过。** 明确存在导入写入原子性、登录身份绑定/有效性、考试批次完整性、学期列表刷新入口和部分状态反馈缺口。真实账号、校园网、定位与设备矩阵仍未完成。

以下位置以仓库根目录为基准，缩写：`C = shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule`，`A = shared/src/androidMain/kotlin/com/xingheyuzhuan/shiguangschedule`。内部 ID、缓存键和技术字段不要求逐个展示为用户字段。

### 功能验收表

| 功能 | 请求 → 服务/存储 → 模型/状态 | 页面 → 操作 → 保存/下次请求 | 分类及边界 |
|---|---|---|---|
| 课程同步 | 内置 XATU 脚本 → `courseTableForStd` → 桥接 → `CourseConversionRepository` → Room → 今日/周课表状态 | 首页同步入口 → 选目标课表/学期 → 写作息、配置、课程；课程详情查看 | **前端存在但链路未完成；需要真实账号验证**：脚本与索引已内置，但覆盖写入缺少外层事务；完整导入不能判通过 |
| 课程名、教师、地点、周次、节次、时间、学分 | 脚本 JSON → `Course` / `CourseWeek` / 作息 → Today/Weekly ViewModel | 今日/周课表 → 课程详情；名称、教师、地点、周次、时间、学分和备注有展示 | **需要真实账号验证**：字段链路齐全，不能以样本测试代替真实导入逐项核对 |
| 课程序号 | `TaskActivity` 解析 → `课程名 | 序号` → 同一 `Course.name` | 课表与详情随完整课程名显示 | **需要真实账号验证**：没有独立序号字段或开关；不是后端已有字段被 UI 丢弃 |
| 当前教学周和开学日期 | 首页教学周请求；脚本按 UTC+8 反推第一周周一，当前学期写 `CourseTableConfig` | 首页/教学周页查询；我的→课表日期与显示可人工编辑 | **需要真实账号验证**：原生查询明确说明不自动改日期；历史学期仍需人工配置 |
| 成绩、学分、绩点、构成、汇总 | 成绩/历史页 → `XatuAcademicService` → `XatuGradePage` → 账号/学期缓存 → `XatuAcademicUiState` | 成绩页选学期/全部历史、搜索、展开构成、刷新；显示课程数/总学分/平均绩点 | **需要真实账号验证**：字段均有 UI；旧的年份误读已有解析回归样本，本轮未复验当前安装包真实数值 |
| 考试 | `stdExamTable` / 批次页 → `XatuExam` → Repository / Academic/Home 状态 | 完整列表显示课程、代码、考试名称、日期时间、考场、座位和备注；首页最近考试 | **前端存在但链路未完成；需要真实账号验证**：只返回首个非空批次，后续批次无选择入口 |
| 学业进度 | `myPlanCompl.action` → `XatuStudyPlan/Section/Course` → 缓存/Academic 状态 | 已修/要求学分、姓名、专业、GPA、分类/子分类、课程通过情况、成绩；搜索与刷新 | **需要真实账号验证**：所建业务字段均有展示位置 |
| 培养方案 | `myPlan.action` → `XatuCourseInfo` → 同一学业快照 | 学业进度→培养方案→全部课程；代码、性质、学分、学时、计划学期、搜索 | **需要真实账号验证**：请求失败有警告并保留旧方案，不伪造已更新 |
| 学期目录刷新 | Repository 已有 `semesters(force=true)` | 页面刷新仅调用无 force 的 `semesters()` | **功能存在但前端缺少展示/操作**：已有强刷能力未接到用户刷新，缓存非空后不会重新请求目录 |
| 校历日期、原图、OCR | 教务处索引/详情/图片 → `XatuCalendarService` / ML Kit → `XatuCalendar` → Campus Flow | 校历页日期、来源、获取时间、本地/在线原图、识别文字/复制、刷新 | **需要校园网验证；前端存在但链路未完成**：并非所有访问失败都是代码错误，但 OCR 抛异常会阻止新原图及快照保存 |
| 电费余额、宿舍绑定 | 缴费平台项目/校区/楼栋/房间/余额 API → `XatuCampusService` → `XatuCampusRepository` | 选择宿舍自动保存，刷新余额，显示绑定信息/查询时间，解绑确认与网页入口 | **需要真实账号验证；需要校园网验证**：字段有来源，单位须与平台核对；下级选择器加载反馈有缺口 |
| 电费历史和趋势 | 成功余额查询追加 `XatuBalanceRecord`，最多 400 条；本机按日汇总 | 最近记录、趋势、全部记录、按日/按次切换；明确标注本机记录 | **需要真实账号验证**：不是平台历史账单，不补造查询前历史 |
| 签到位置与权限 | Android 定位/手填 → 坐标校验转换 → Campus DataStore → 受限签到桥接 | 权限/系统定位状态、当前位置、坐标系、保存、启停、网页签到 | **需要定位权限验证；需要实体设备验证；需要真实账号验证**：原生不宣称已取得学校签到结果 |
| 教务登录与验证码 | 正常 WebView + JS/DOM Storage/第三方 Cookie → 统一认证；完成登录调用 SessionRepository | 我的→教务账号→网页登录→完成登录 | **前端存在但链路未完成；需要真实账号验证；需要实体设备验证**：当前以教学周判定登录，且未核对服务器身份与手填学号 |
| 账号切换/退出 | Session clear → 清 WebView/Cookie → 清对应学术缓存、Campus 快照、账号 DataStore | 切换需先退出；退出成功/失败明确显示；保留已导入本地课表 | **需要真实账号验证**：有清理链路和 Campus 清理测试，但不能判跨账号/重登端到端通过 |
| 会话失效 | Service 检测登录页 → `XatuSessionExpired` → 页面 `requiresLogin` | 页面重新登录按钮可见；账号面板仍只读持久化 loggedIn | **前端存在但链路未完成**：服务失效与账号面板“教务已连接”的状态不同步 |
| 服务之间的 WebView 会话 | 共享 Controller、CookieManager 和 CookieJar；离页仅 detach/flush | 网页服务复用实例与 Cookie；主动退出才清理；主框架错误可重载 | **需要真实账号验证；需要校园网验证；需要实体设备验证**：跨域 SSO、验证码、服务连续切换和重启恢复未本轮实测 |
| 校园网页入口 | 一站式、请销假、图书馆、校园网、电费充值、教务、一网通办 URL | 校园→更多网页服务，标注“网页服务”，进入共享 WebView | **需要校园网验证；需要真实账号验证**：没有假装为原生查询；URL 是否仍有效必须在实际环境验证 |
| 三风格偏好与业务一致性 | AppSettings `ui_style` → 设置 Flow → 固定应用主题树 → 共享页面/组件 | 我的→界面与主题，保存状态明确；浅/深/跟随系统独立 | **已完成（源码/持久化测试范围）；需要实体设备验证**：未发现按风格分支的业务请求或字段过滤；不是全控件原生库替换 |
| 应用名、图标、启动页、系统入口 | Manifest label/icon/theme → 多语言资源；同一品牌 mark 用于通知/启动/应用内 | AAPT2 确认中文一校通、英文 XATU One；Application ID `com.xatuone` | **已完成（主要资源与 APK metadata）；需要实体设备验证**：残留旧商店图标和重复打包资源见下文 |
| 原生学校签到结果/提交、平台电费历史账单、结构化假期自动应用 | 当前没有对应已验证原生数据链路 | 签到/缴费已有真实网页；当前说明未承诺这些原生能力 | **当前不应继续开发**：本轮先补已确认缺口，未取得真实契约前不要猜接口或造数据 |

### 已确认缺口与最小修补文件

| 优先级 | 具体触发条件与影响 | 源码证据 / 后续最小修补范围 |
|---|---|---|
| P1 | 覆盖导入在删旧课后、课程或周次插入完成前失败/取消，可能留下空表或部分数据；前端提示失败不等于旧数据已回滚 | `C/data/repository/CourseConversionRepository.kt:149-208` 在普通 Repository 上标 `@Transaction`，但没有数据库事务作用域；生成 `CourseDao_Impl.kt:156-158,776-787` 的删除和插入各自调用 `performSuspending`。应将同次覆盖操作放入真实 Room 数据库事务，补失败回滚测试；不改变格式/业务规则。未对真实课表做故障注入 |
| P1 | 手填学号与网页登录身份不同，仍可被标为已登录，并以手填学号作为缓存键；合法登录但教学周缺失又会被拒绝完成登录 | `A/ui/settings/xatu/XatuPortalScreens.android.kt:92-96` 保存手填学号；`A/xatu/AndroidXatuSessionRepository.kt:17-20` 只检查 officialWeek 后 markLoggedIn。后续应复用教务真实身份信息核对/绑定账号，登录有效性与教学周可用性分开；不得猜新接口 |
| P1 | 同学期有多个非空考试批次时，首批后的考试不进入模型，也没有批次入口；首页/完整列表可能均不完整 | `A/xatu/academic/XatuAcademicService.kt:70-88` 首个非空结果直接 return；UI `A/ui/settings/xatu/XatuServicePage.android.kt:716-743` 无批次操作。应先取得真实多批次样本确定合并/去重规则，再修 Service；不能称已实测漏掉某场具体考试 |
| P2 | 学校增加或修正学期目录，用户点刷新也仍使用持久化旧目录；`force` 能力实际上不可操作 | `A/xatu/academic/XatuAcademicRepository.kt:37-41`；`A/ui/settings/xatu/XatuAcademicViewModel.android.kt:139`、`XatuHomeViewModel.android.kt:75` 均无 force 调用。把已有明确刷新动作接到目录强刷即可，不必新增页面/缓存框架 |
| P2 | 会话过期后服务页要求重新登录，账号页仍显示“教务已连接” | `A/ui/settings/xatu/XatuAcademicViewModel.android.kt:83-84,108-109`、`XatuHomeViewModel.android.kt:86-87` 只改局部 requiresLogin；`XatuLoginScreen.android.kt:55` 依赖持久化 loggedIn。统一失效状态的传递，保留课程/缓存，不将失效处理变成静默清数据 |
| P2 | 请求校区、楼栋、房间期间，空列表直接显示“暂无选项”；失败后改选并成功返回时，旧错误可能继续留到下一次状态写入 | `A/ui/settings/xatu/XatuServicePage.android.kt:434-466,506-515,646-655`。各级请求没有对应 loading/success-empty；只对项目有 projectsLoading。补足本页现有状态，成功清理对应旧错误；无需搬迁业务或新增架构层 |
| P2 | 刷新同学期成绩会更新共享 snapshot.fetchedAt，缓存中的考试未更新，但考试页/首页仍可能用新时间标记旧考试 | `A/xatu/academic/XatuAcademicModels.kt:86-94`；`XatuAcademicRepository.kt:44-68`；`XatuAcademicViewModel.android.kt:151-159`；`XatuHomeViewModel.android.kt:66-71`。至少明确区分“快照最近写入”与“本项数据刷新”；若产品需要每项时间，再对兼容可空时间字段单独设计，不直接改旧缓存格式 |
| P2 | 图片已下载，但 OCR 失败时新原图也不可缓存/查看；用户只得到校历整体失败 | `A/xatu/campus/XatuCalendarService.kt:24-54` 在保存图片和返回快照前同步执行 OCR。最小修补应分离原图成功与 OCR 失败反馈，保留可查看的真实图片并明确报告识别失败，不吞异常、不伪造日期。校园网访问受限本身不列为代码缺陷 |
| P3 | 商店图标源文件仍为旧蓝底课表/白熊图，和当前门形品牌不同 | `androidApp/src/main/ic_launcher-playstore.png` 已实际查看；`fastlane/metadata/android/en-US/images/icon.png` 为新品牌。发布前替换或移除旧商店图标，并核对发布使用路径；当前 Launcher 引用的是新 adaptive icon，不把此项误称运行时桌面仍旧图标 |
| P3 / 待定位 | 当前 Release 同时包含 `shiguangschedule.shared.generated.resources` 与旧 `xatuone.shared.generated.resources` 两份离线 ZIP，后一份无 XATU；存在重复和过期资源 | ZIP 检查已确认；生成 `Res.kt` 指向前者且脚本与当前源码一致，因此不是当前主路径缺脚本。后续在隔离构建输出中查清来源，再修对应任务/资源纳入路径；优先查 `shared/build.gradle.kts:16-18,150-166`、`androidApp/build.gradle.kts`，不能先改业务或清用户工作区 |

异步补充风险（尚未用当前 APK 复现）：Academic ViewModel 取消旧请求后启动新请求，两个请求的 finally 均写同一 loading；需要针对快速换学期验证旧任务结束是否提前解除新任务的加载态。见 `XatuAcademicViewModel.android.kt:63-89,98-114`。当前不据此断言已经观察到错误数据覆盖。

### 后端字段展示与真正未接入能力

- 成绩的 `credit/score/gpa/term/parts` 和 `summary.count/credits/gpa` 均在 `XatuServicePage.android.kt:676-710` 消费；考试的非技术字段均在 `:716-739` 消费。问题重点是请求完整性、刷新与账号状态，不是补一套新的列表。
- `XatuStudyPlan` 及分类/子分类课程在 `:747-792` 消费，`XatuCourseInfo` 的性质/学时/计划学期在 `:795-821` 消费；`XatuCalendar` 的原图/识别文字/日期/获取时间在 `:832-891` 消费。
- 宿舍名称、余额、时间、全部本机记录和按日汇总在 `:493-619` 消费；坐标、坐标系、启停、权限和保存反馈在 `:229-342` 消费。
- 主要 XATU 业务范围内，未确认存在一整项已成功取得并持久化、却完全没有消费页面的功能。发现的明确孤立能力是学期目录强刷参数；`XatuCampusService.loadPage()` 也无调用方，但只是通用辅助函数，不应为它强行增加产品入口。
- 独立序号字段/显示开关、结构化假期写入、原生签到结果和平台历史账单没有现成完整后端；不能按“UI 漏显示”处理。当前真实网页和已有文字说明应保留。

### 三风格、品牌与既有体验改进

- 三风格共用入口、字段和回调；风格分支位于视觉组件/主题层。换肤只写 `ui_style`，不调用 Cookie、账号、缓存或课表清理；`UiCard` 保留内容子树，导航不以风格为键。真实切换后的焦点、滚动、导航和 SSO 仍需设备验证。
- 首页依次呈现当前教学周、今日课程、最近考试、常用服务；校园页有分组和搜索；我的页集中账号、主题与课表配置。成绩构成、培养方案全文、本机电费历史、校历文字均有直接入口，网页服务明确标注。
- 本轮 AAPT2 确认当前 Release：包名 `com.xatuone`；默认/简繁中文 label 为一校通，英文为 XATU One。Manifest 的 Launcher/roundIcon 指向同一 adaptive icon，启动页使用同一图标；通知使用同一品牌 mark 的资源别名，小组件和分享标题已更新。
- 对当前 Android/共享资源与代码、README/fastlane 的定向检索，未发现仍以 XATUHelper 或拾光作为用户可见应用标题。剩余的 XATUHelper 名称出现在 MIT 许可/来源注释；拾光出现在项目来源/适配协议说明；旧 Kotlin namespace 和桥接名属于内部兼容标识。不得为品牌清理删除许可或无依据改包名。旧商店图标和重复产物另列，不能宣称“零残留”。
- COUI 使用原生开关，但通用输入、弹窗及深层 Material 控件仍走共享适配；这属于明确的集成边界，不是另做一套业务页面或隐藏字段。

### 验证记录与真实环境待办

复用上一轮、源码输入未变的命令：

```text
gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:lintRelease :androidApp:assembleRelease --offline --console=plain
```

结果为 BUILD SUCCESSFUL；现存 test XML 汇总为 50 tests / 0 failures / 0 errors / 0 skipped；Release lint 328 warnings / 0 errors；R8 Release 已生成。测试涵盖解析、当前学期推断、课程渲染、Campus 写入隔离、导航和风格 DataStore 重开，但没有真实 API/E2E 或课程覆盖失败回滚测试。本轮没有将这些结果称作真实功能验收通过。

本轮实际执行：

- `adb devices -l`、`dumpsys package com.xatuone`、`pm path` 与安装包/工作区 SHA-256 比对，仅检查安装状态，没有清 Cookie 或改账号。
- `aapt2 dump badging ...x86_64-release.apk`：确认名称、包名、minSdk 和入口。
- APK ZIP 检查：当前 Res 路径含 XATU 脚本且字节与源码一致；同时记录重复旧命名空间资源。
- 当前源码/调用方检索、Room 生成 DAO 核查、两张本地图标实际查看、已有测试 XML 和 lint 报告读取。
- `git diff --check`：通过。本轮仅增补本文件。

| 真实环境 | 尚待完成的验收 |
|---|---|
| 真实账号 | 当前工作区 APK 的完整导入及原教务逐字段对照；成绩汇总/各学期/全部历史；非空及多批次考试；学业/方案；手填学号与网页身份一致性；A→退出→B 无旧缓存/网页泄露；登录过期再登录 |
| 校园网 | 校历索引/详情/原图/OCR；缴费平台登录与余额单位；受限教务、图书馆、校园网充值、一站式等完整网页流程。无权访问本身不归因于接口损坏 |
| 定位权限 | 首次授权、拒绝、仅大致、精确、设置页恢复和系统定位关闭；WGS84/GCJ02 配置及网页接收 |
| 实体设备 | 三风格×明暗模式×窄/宽/大字号；焦点/键盘、TalkBack、点击目标、安全区域；验证码/iframe/重定向；服务连续访问、进程重建和 App 重启后会话；厂商桌面/任务列表/通知/启动页品牌表现 |

### 后续最小修补顺序

1. 先修课程覆盖导入真实事务，并验证插入失败仍保留旧课表；不在真实课表上注入故障。
2. 绑定教务真实身份，分开登录有效性与教学周，统一过期状态；以真实 A/B 账号验收隔离，不复制账号框架。
3. 用真实多批次响应确定考试规则；接通已有学期目录强刷；修正缓存时间展示语义。
4. 补齐电费下级请求状态；让校历原图成功与 OCR 失败能分别呈现，保留原有数据模型/接口优先。
5. 发布前处理旧商店图标、定位重复资源来源；随后在与工作区一致的 APK 上完成真实环境矩阵。暂不扩展原生签到、历史账单或猜测接口。

## 2026-11-07 最终构建检查与交付清理

### 本轮范围与修改

- 分支 `main`。开始时已有 130 个未提交路径，均保留；没有提交、推送、发布或安装操作，也没有 reset/clean 等 Git 操作。
- 删除 `androidApp/src/main/ic_launcher-playstore.png`：旧蓝底课表/白熊商店图，无构建或发布引用。保留已存在的新品牌 `fastlane/metadata/android/en-US/images/icon.png`。
- 删除 `shared/src/androidMain/kotlin/com/xingheyuzhuan/shiguangschedule/xatu/campus/XatuCampusService.kt` 中无调用者的 `loadPage` 方法，不调整实际电费请求。
- 更新本报告。没有修改依赖、业务接口、数据格式、应用包名或非 Android 平台代码。
- 旧 `xatuone.shared.generated.resources` 来自遗留增量构建输出。通过 Gradle 的 `:shared:clean :androidApp:clean` 清理构建产物后重新构建，未增加永久清理任务或资源排除规则。六个最终 APK 均不再包含旧命名空间；每包只有一个离线学校 ZIP，其中 XATU 脚本与当前源码逐字节一致。

### 实际运行的构建与检查

构建使用 JDK 21、Android SDK 和本机 Gradle 缓存。

```text
gradlew.bat :shared:clean :androidApp:clean :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:lintRelease :androidApp:assembleRelease --offline --console=plain
git diff --check
```

- Gradle `BUILD SUCCESSFUL`，耗时 2 分 24 秒；Debug、Release 各生成 arm64-v8a、armeabi-v7a、x86_64 三个 APK。Release 执行 R8 和资源压缩。
- Host tests：10 个测试套件、50 项测试，0 失败、0 错误、0 跳过。包含三风格偏好存储重开后恢复、其他偏好保持、导航状态及业务解析测试；不等于 UI 实机测试。
- Debug / Release lint：各 0 错误、310 条警告。主要为小组件布局一致性 138、文本尺寸 70、无障碍描述 25；另有依赖版本 16、未引用资源 15 等。离线版本检查与此前联网报告不可直接比较，不将警告数变化宣称为质量改进。
- 编译仍有 expect/actual Beta、已弃用 API、Koin 某次编译未执行入口校验等警告，没有通过屏蔽警告制造“零警告”。
- 15 项疑似未引用资源为 `colors.xml` 中 8 项、原生 `strings.xml` 中 6 项和 `widget_divider_horizontal.xml`。本轮记录但未扩展为小组件/多语言资源整体清理；无资源合并冲突。
- `git diff --check` 通过；Git 提示部分文件 LF 将转 CRLF，未改行尾策略。差异中没有构建输出或 iOS/Desktop 平台改动。
- AAPT2 检查 Debug/Release arm64-v8a 的标签、Manifest 和资源表；Release 不是 debuggable。apksigner 检查：Debug 签名有效，**本地 Release 未签名**，不能直接作为正式安装发布包。现有 CI 通过签名参数注入正式签名；本轮未触发 CI 或发布。

### 品牌、主题和功能入口

| 检查项 | 结论与验证范围 |
|---|---|
| 应用身份 | APK 中文标签“一校通”、英文“XATU One”；Application ID `com.xatuone`，版本 2.1.0 / 36，未修改 |
| Launcher / Splash | APK 中 adaptive icon 有背景、前景及 monochrome 层；前景与通知共用品牌 mark；`Theme.XatuOne` Splash 引用同一 Launcher 标识，浅/深背景均打包。商店新图已实际查看；厂商裁切和启动动画尚未实机验收 |
| 旧名称 | 未发现以旧品牌作为应用显示名或 XATUHelper 业务标题。保留来源说明、MIT 署名、适配仓库地址、内部命名空间/桥接协议/密钥别名；WebDAV 缺省旧目录 `ShiguangSchedule` 也保留，避免改变既有备份定位，新的配置模型缺省为 `XATU One` |
| 三套风格 | 我的→界面与主题可选 Material 3 / MIUIX / COUI；单一 `ui_style` 偏好、固定 Provider 树、共用导航与业务状态。持久化及状态自动化测试通过；未在当前 APK 上实测触控切换、键盘及 WebView 会话 |
| 风格限制 | MIUIX 0.9.4、COUI alpha7；COUI 的部分输入框、弹窗、日期/颜色选择器为共享 Compose 控件，页面已有适配说明。网页和系统权限界面不换肤，不能称全部组件均为各库原生实现 |
| 核心入口 | 源码确认首页、今日/周课表、设置、账号、同步、成绩、考试、学业进度/培养方案、教学周/日期、校历、电费、签到位置和校园网页入口均有导航处理；仅确认源码与编译，不宣称每页已在设备打开 |
| 未实现或受限能力 | 网页入口标注“网页服务”，电费历史标注“本机历史”，签到为位置配置与网页签到；没有冒充原生签到结果或平台账单历史。前述审计中的完整性/状态缺口仍存在，不能将所有入口判为完整可用 |
| 假数据 / 调试内容 | 生产源码检索未发现假结果、临时测试页面或测试入口；不能据此证明不存在任何死代码。默认关闭的开发者模式为既有适配工具，保留；Debug 包含预期 Compose tooling，Release 未声明该调试依赖 |

### 未完成及真实环境边界

- 前述 P1：课程覆盖写入外层事务、教务身份绑定/登录有效性、考试多批次完整性仍需修补。
- 前述 P2：学期目录强刷、会话过期账号状态同步、电费下级加载反馈、缓存时间语义、校历原图与 OCR 失败分离仍待处理。
- 真实账号/验证码：真实导入和字段对照、成绩汇总、非空/多批次考试、账号隔离、会话过期重登尚未本轮验收。
- 校园网：校历/原图/OCR、缴费平台、受限教务及校园服务实际响应与余额单位尚未本轮验收；无权访问不能直接判为代码缺陷。
- 定位权限：拒绝/大致/精确授权、系统定位关闭及网页接收尚需验证。
- 实体设备：三风格即时切换和重启恢复、核心页面逐一打开、窄/宽屏/大字号、TalkBack、键盘、安全区域、厂商图标/Splash/通知，以及跨服务和重启后 WebView 会话，均未在本轮安装验证。

构建日志仅保存在本机；测试报告：`shared/build/reports/tests/androidHostTest/index.html`；lint 报告：`androidApp/build/reports/lint-results-debug.html`、`lint-results-release.html`。本轮结论为**构建与限定清理完成，完整功能验收和正式发布条件尚未完成**。

## 2026-11-07 后续模拟器验证（用户授权）

用户随后要求“在模拟器验证”，因此本轮使用 ADB 对 `emulator-5554`（API 34、x86_64）覆盖安装当前 Debug APK，未操作实体设备、卸载应用或清除应用数据。安装包和设备 `base.apk` 的 SHA-256 均为 `00557aaef63837c123567ab2a6730f9aa06f136edb736130bcecaf858ab487c8`，确认不是旧安装版本。

### 实际运行结果

| 项目 | 模拟器观察结果 | 边界 |
|---|---|---|
| 应用启动和导航 | 首页、今日安排、周课表、我的、主题、账号、校园门户、成绩、考试、学业进度、教学周、电费、校历、签到位置和校园 WebView 均实际打开；服务页系统返回回到门户，未复现跳到“我的” | 并非每个页面均完成所有操作，也未完成三风格全页面矩阵 |
| 三套风格 | COUI→Material 3→MIUIX→COUI 均即时生效，切换停留在主题页；浅/深设置与风格分开，COUI 深色已截图 | 有 COUI 账号信息缺失问题，不能判功能展示完全一致 |
| 偏好恢复 | MIUIX、COUI 分别强制停止并重新启动，设置页仍显示对应风格，账号记录保留 | Material 3 本轮仅测试即时切换，重开持久化仍以既有自动化测试为证据 |
| WebView 复用 | 一站式入口实际加载统一身份认证页面，背景、账号/密码输入、验证码登录标签和脚本页面可见；网页离页后由 COUI 切换至 MIUIX，DevTools target ID 和 `performance.timeOrigin` 都未改变，说明该文档未因换肤销毁或重载 | 未输入账号密码或提交验证码；当时页面为登录页，不能验证有效登录 Cookie 的跨域、重启恢复 |
| 课程同步入口 | 实际打开“西安工业大学→树维教务系统适配”，未再出现暂无适配器 | 未执行导入写入，不覆盖用户课表；当前课表未配置开学日期，今日/周课表仅验证当前空/未配置状态 |
| 成绩 | 缓存列表与汇总显示，平均绩点与汇总行一致，年份误读已修复 | 当前实时请求失败；没有逐课对照官方系统 |
| 考试和学业 | 考试默认 2026–2027 学年 1 学期；缓存课程序号/考场有展示。学业页可见已修/要求学分、专业和 GPA | 未验证真实日期、座位字段及多考试批次；缓存非完整考试验收 |
| 电费 | 已绑定宿舍、余额、查询时间、1 条本机记录可见，明确提示至少两次记录后显示趋势、不包含平台历史账单 | 未重绑/解绑、造记录或验证趋势曲线；下级选项仍显示暂无选项 |
| 校历 | 页面能打开，实际显示“校历正文未找到原图”，有刷新和来源网页入口 | 本环境未获得可用原图，未验证 OCR；不能单凭此结果认定校园网内同样失败 |
| 签到 | 页面显示未授权、系统定位开启、自定义位置未启用，坐标系与输入区可见 | 未授权定位、修改坐标或提交学校签到；未验证真实定位 |
| 品牌 | Android 应用信息页实际显示“一校通”和新绿底门形图标 | 冷启动抓帧未捕获到 Splash，不能将旧资源引用检查升级为启动动画视觉验收 |
| 布局样本 | MIUIX 首页检查 320dp 宽/1.3 倍字体及 640dp 横屏，主导航和页面可呈现；常规尺寸为 360dp 宽 | 仅首页样本，低屏高需要滚动；不是全部页面、全部主题、TalkBack 的无障碍验收 |
| 崩溃 | 检查结束时 ADB crash buffer 无条目，所操作路径未见崩溃 | 不能证明所有业务路径无崩溃 |

### 本轮发现的具体问题

1. **实时教务请求阻断**：首页、成绩、考试、学业、教学周均观察到 `CLEARTEXT communication to authserver.xatu.edu.cn not permitted by network security policy`。`XatuAcademicService.kt:25-37` 的请求在取得 HTML 前就因认证跳转失败，未转成已有的 `XatuSessionExpired`；`androidApp/src/main/res/xml/network_security_config.xml` 仅允许教务域名 HTTP。应优先检查过期会话重定向的识别和 HTTPS 重新认证链路，不能为通过测试而全局放开明文认证通信。
2. **COUI 账号字段实际丢失**：常规 360dp 宽、默认字体下，COUI 账号页只剩“切换”和“退出登录”，没有当前账号及连接状态；切换 MIUIX 后同一账号字段可见。复核位置 `XatuLoginScreen.android.kt:49-76` 的加权文字列与尾部操作，以及 `UiFormComponents.android.kt:25-42` 中使用全宽 COUI `Item` 的按钮适配。应修共享组件的尺寸契约，不复制业务页面。证据截图 `35-account.png` / `50-miuix-account.png`。
3. **今日页未配置状态不一致**：当前开学日期缺失，首页提示无法确定今日课程；今日安排页同时显示“请设置开学日期”和“今天没有课程”。后者不能当作已确认无课，后续应让未配置状态与成功空课表明确区分。证据截图 `40-today.png`。

### 交接状态

- 本轮未修改正式 UI、业务逻辑或依赖，仅更新本验收记录；不重复运行输入未变化的构建/测试。没有提交、推送或发布。
- 未主动退出账号、清除 Cookie、清空缓存、同步覆盖课程、修改宿舍或签到位置。最后恢复原先 COUI＋跟随系统，模拟器尺寸恢复 1440×2560、默认字体 1.0，临时 DevTools 端口转发已移除，应用留在首页。
- 仍需真实账号重新登录后验证实时课程/成绩/考试与 Cookie；仍需校园网验证校历、电费和校园服务；仍需权限和实体设备验证定位、验证码挑战、厂商启动页及完整无障碍矩阵。
- 截图、UI XML 和 WebView 对照证据保存在本机验收目录，包含个人教务缓存，不作为公开发布资料。

## 2026-11-07 模拟器确认问题的三项限定修复

### 范围与实现

开始时 `main` 已有 131 个未提交路径，保留全部既有改动；本轮只修改以下四个源码/测试文件并追加本记录，没有提交、推送或发布。

| 文件 | 本轮修改及触发条件 |
|---|---|
| `shared/src/androidMain/kotlin/com/xingheyuzhuan/shiguangschedule/xatu/academic/XatuAcademicService.kt` | 教务过期会话跳转 HTTP 认证域名时，在 OkHttp 跟随跳转前，将确切的 `authserver.xatu.edu.cn` Location 升级为 HTTPS，保留路径及 service 参数。派生客户端继续使用原 CookieJar、连接池和默认 TLS 校验。 |
| `shared/src/androidMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/settings/xatu/XatuLoginScreen.android.kt` | COUI 全宽按钮放在账号标题横排中会挤掉加权文字列；将切换与退出操作放到标题下方的 FlowRow，让账号和连接状态独占标题可用宽度，保留原回调与进行中禁用条件。 |
| `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/today/TodayScheduleScreen.kt` | 空列表仅在 `TodayStatus.Normal` 下显示“今天没有课程”，其余状态显示对应的未配置/假期/学期结束说明，不调整周次计算或课程查询。 |
| `shared/src/androidHostTest/kotlin/com/xingheyuzhuan/shiguangschedule/xatu/XatuCoreTest.kt` | 新增两项重定向测试：HTTPS 升级保留 service 参数；已有 HTTPS、其他主机、伪后缀域名和普通相对跳转不被改写。 |

未修改网络安全 XML、Manifest、数据模型、存储格式、外部接口或课程业务规则；没有处理本轮范围外的校历、OCR、导入、验证码、定位、跨账号隔离和 Splash。

### 网络链路与真实请求

- 调用链为页面 `XatuAcademicViewModel` → `XatuAcademicRepository` → `XatuAcademicService.page` → OkHttp。服务基址为 `http://jwgl2018.xatu.edu.cn/eams/`；成绩使用 `teach/grade/course/person.action`、`person!search.action` 或 `person!historyCourseGrade.action`，考试使用 `stdExamTable.action` / `stdExamTable!examTable.action`，学业使用 `myPlanCompl.action` / `myPlan.action`，教学周使用 `homeExt!main.action`。成绩和考试初始化还会先获取学期/当前教学周，因此本次过期会话可能在到达成绩或考试数据端点前就失败。
- 无凭据真实探测教学周 URL，返回 302 到 HTTP `authserver.xatu.edu.cn/authserver/login`；该 HTTP 认证地址本身返回 302 到同主机 HTTPS。直接请求 HTTPS 认证地址返回 200，使用系统信任库验证证书成功。
- HTTPS 教务主机本次探测超时，不能据此认定服务永远不支持 HTTPS。保留教务主机原有 HTTP 许可，认证跳转使用已实际验证的 HTTPS；不新增认证域名明文许可、不全局允许 HTTP、不忽略 SSL 错误。
- 在更新后的模拟器 App 中实际打开并刷新成绩、考试、学业和教学周，最终均明确显示“教务登录已失效，请重新登录”，未再出现 `CLEARTEXT` 阻断。原有异常只会在取得登录 HTML 后产生，源码和真实运行结果共同确认已越过原网络策略阻断。
- **这不是实时成绩/考试/学业数据验收通过**：现有远端登录失效，缓存时间没有因此变为成功刷新时间；未重新登录，也没有用缓存内容替代实时查询证据。有效账号下的业务数据查询仍需重新登录后验证；校园网内服务响应未在本轮验证。

### 模拟器 UI 验证

环境为 `emulator-5554`、API 34、x86_64、360dp 宽；覆盖安装本轮 Debug APK，没有卸载或清除数据。

| 验证项 | 实际结果 |
|---|---|
| COUI 默认字体 1.0 | 账号完整显示，连接状态和切换/退出按钮可见，无重叠、裁切；已查看截图。 |
| COUI 系统字体 1.5 | 同一账号和连接状态仍完整可读，操作按钮另起行；点击切换可进入编辑/网页登录区域。退出按钮保留可点击状态，未实际退出账号。 |
| 今日页未设置日期 | 只显示“请设置开学日期”的未配置状态，不出现“今天没有课程”。 |
| 今日页已设置日期 | 通过设置页临时选择 2026-11-02，现有空课表正确显示“第 1 周”和“今天没有课程”，不显示缺日期提示；没有新增课程或伪造服务数据。 |
| 恢复原状态 | 停止 App 后只将当前课表的 `semesterStartDate` 字段恢复为原始 NULL，查询确认恢复，再次打开今日页仍为未配置提示；原有账号、Cookie、课程和缓存未清除。恢复本轮开始时的 MIUIX＋跟随系统及字体 1.0，应用留在首页。 |

本次只覆盖现有空课表的两种日期状态；非空课程列表逻辑没有修改，未新增课程来宣称完成非空课表的设备回归。实体设备及其他字体/宽度组合未在本轮验收。

### 自动化结果与证据

```text
gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain
git diff --check
```

- Gradle `BUILD SUCCESSFUL`，1 分 7 秒；10 套、52 项 host tests，0 失败、0 错误、0 跳过。
- Debug lint：0 错误、310 条警告，未扩展为既有警告清理；没有因测试输入未变化重复运行构建。
- 本轮源码和文档的 `git diff --check` 通过；没有修改构建输出到版本控制、非 Android 平台或无关模块。
- 构建日志、网络探测及 UI 截图/XML：本机验收目录。账号截图包含个人信息，仅留本机，不作为公开发布附件。


## 2026-11-07 信息架构重设计与导航实施

### 本轮完成

- 一级导航调整为首页／课程／教务／校园／我的；保留 Navigation 3 已有主页面状态管理，不新增平行业务状态或重复页面。
- 课程顶部新增今日、同步课表、课表设置；独立课表设置集中日期、周次、显示、作息、课程外观、假期与管理工具。同步固定入口不再散落于首页和转换页。
- 教务目录集中成绩、考试、学业、教学周、校历与教务网页；校园目录集中电费、签到及其他真实网页服务，位置配置独立在服务设置组。
- 我的保留账号、外观主题、通知、隐私、网络、备份及应用信息；课表配置移出。隐私和网络新页仅提供真实说明与系统设置操作。
- 首页依次呈现日期／周次、今日课程、已绑定宿舍电费摘要、最近考试及网络说明；电费复用现有快照并显示查询时间，不新增请求或实时数据承诺。
- 移除不再使用的首页服务网格函数及其列数测试；补充新目的地序列化、课程设置逐级返回、教务／校园按归属返回测试。
- 完整页面树、设置归属、用户路径、入口变化和源码文件范围见 `ui-ux-plan.md` 当前结构章节；同步更新 `architecture.md`。

未修改 API、存储格式、数据模型、课程规则或 WebView 会话管理。保护此前未提交改动，没有提交、推送或发布。

### 自动化验证

```text
gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain
git diff --check
```

- 联合构建成功，1 分 29 秒；Android 编译和 Debug APK 构建通过。
- 10 套、53 项 host tests，0 失败、0 错误、0 跳过。
- Debug lint：0 错误、310 条警告，未扩大为无关警告清理。
- 本轮按导航／页面结构修改运行 Debug 检查，没有重复执行 Release 签名或发布流程。

### 模拟器验证

只覆盖安装到 `emulator-5554`（API 34、x86_64），未安装到实体设备，未清除应用数据。

| 路径／场景 | 观察结果及边界 |
|---|---|
| 五项导航、教务与校园目录 | 新入口按归属显示；校历详情系统返回教务目录，没有跳转“我的”。校园网页明确标注网页服务。 |
| 课程 → 课表设置 → 日期与周次 | 页面可打开，显示当前课表名称；系统返回先到课表设置，再回课程。未修改开学日期或课程数据。 |
| 课程 → 同步课表 | 打开现有 XATU 适配器列表，显示树维适配卡。未点击执行脚本、登录或覆盖导入，不能据此认定真实导入通过。 |
| 我的 → 隐私／网络 | 两页可打开并返回“我的”，系统设置按钮由实际 Android Intent 实现；未修改系统权限或网络。 |
| Material 3／MIUIX／COUI | 通过现有外观设置切换，五项导航、课程顶部三操作与“我的”分组均显示；未创建三份页面。不是所有深层业务的三风格回归。 |
| COUI、320dp、字体 1.5 | 五项导航可读，课表设置可进入和返回，服务目录可滚动。课程顶部四字操作换行，仍可点击；原周课表日期网格有文字裁切，未宣称大字号课表布局全部通过。 |
| 640dp 横向、MIUIX | 页面和五项底栏可呈现，低高度需要滚动。本次未完成侧栏模式验收；额外尺寸切换出现模拟器桌面进程异常，不属于 App 崩溃证据。 |
| 恢复状态 | 尺寸恢复 1440×2560、字体 1.0、MIUIX＋跟随系统，App 留在首页。没有主动退出账号、清 Cookie、改宿舍或签到位置。 |

截图/XML 和构建日志位于本机验收目录，含个人缓存信息，仅保留本机。现有远端登录失效，校历本次仍提示读取失败；未用缓存替代实时数据验收，也未扩大本轮范围修接口。

### 剩余事项

- 真实账号重新登录、验证码、真实课程导入和跨服务 Cookie 连续使用仍需单独验收；本轮没有变更相应实现。
- 校历、电费和其他校园网服务的业务成功结果仍需具备访问条件的网络；首页余额只代表最近保存结果。
- 周课表大字号日期网格、完整 TalkBack 交互、所有机型安全区及侧栏模式尚未完整验证。
- 课程序号暂无独立显隐字段；本轮保留现有导入名称后缀。电费原始单位尚未确认，不擅自换算为度。

## 2026-11-07：首页校园门户重构

本节补充并更新上一轮信息架构验收：首页现在主动查询真实电量，单位已根据 `querySydl` 及参考实现确认是剩余电量，统一显示“度”，不作数值换算。

### 页面与数据链路

- 首页顺序为账号／日期／教学周、今日课程与日期／同步操作、宿舍电量与趋势、最近考试、四项常用操作、教务异常及网络说明。没有接入校园动态 API，因此不展示虚构动态。
- 课程摘要复用 `TodayScheduleViewModel`；未配置开学日期时只提示设置日期，不断言今天无课。课程整卡可进入今日安排，同步进入原 XATU 适配流程。
- 电量首次创建及宿舍改变时请求一次，手动刷新仍使用 `XatuCampusService.electricityBalance`，保存到电费详情观察的 `XatuCampusRepository`。`collectLatest` 取消旧宿舍的任务，失败保留旧记录并展示错误与时间。没有新增定时电量轮询。
- 首页、详情共享 `BalanceTrend`，仅展示最近七次实际查询，按真实时间间隔绘图。无记录／单条记录分别显示空状态；增加、减少、持平均根据最近两条真实记录计算，不冒充逐日用电量。
- 教务摘要区分首次读取、实时更新、缓存、网络错误和登录失效；考试缓存标明来源时间，不把缓存当作实时请求成功。
- 首页按实际底栏高度限制滚动视口；宽度足够时课程与电量并排，字体放大时退回单列。模拟器确认 COUI 账号按钮会占满横向空间，已在首页限制该按钮宽度，保留账号和状态文字的可用空间。
- 三套主题共用上述页面、状态和业务目的地，没有新增 Cookie 清除、WebView 重建或业务数据迁移。

### 修改范围

- 首页与状态：`XatuHomeScreen.android.kt`、`XatuHomeViewModel.android.kt`、`XatuNetworkNotice.android.kt`。
- 共享趋势和单位：`XatuBalanceTrend.android.kt`、`XatuElectricityHistory.kt`、`XatuServicePage.android.kt`。
- 底栏高度传递：`NavigationComponents.kt`、`App.kt`、`XatuLoginScreen.kt`，仅首页消费新增边距。
- 测试与文档：`XatuHomeOverviewTest.kt`、`architecture.md`、`ui-ux-plan.md`、本报告。以上文件可能同时包含前几轮未提交修改，本轮没有覆盖或整理既有改动。

### 自动化验证

```text
gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain
git diff --check
```

- 修复 COUI 挤压后的最终联合构建成功，55 秒；55 项 host tests，0 失败／错误／跳过。
- Debug lint：0 error、310 warning；未清理与本轮无关的既有警告。
- 新测试覆盖真实历史乱序、缺失日期、最近七次、余额增加／减少／持平，不填补日期、不改变源列表。
- `git diff --check` 通过；没有提交、推送、发布或安装到实体设备。

### 模拟器观察

在 API 34 x86_64 雷电模拟器覆盖安装 Debug，保留应用数据。

| 场景 | 观察结果与边界 |
|---|---|
| 电量实时请求 | 首次查询和手动刷新成功，查询时间与真实历史新增；后来取得新的减少读数，首页显示对应差值。不是仅查看缓存。 |
| 摘要进入电费详情 | 完整记录打开原详情页，与首页同一剩余电量值，返回首页保留滚动位置。 |
| 同步课表／今日课程 | 首页同步打开现有 XATU 树维适配器列表；课程摘要进入今日安排，返回原首页。未执行覆盖导入。 |
| 登录失效 | 真实教务会话已失效，首页展示登录失效、重新登录入口及缓存时间，没有宣称教学周／考试实时更新成功。 |
| Material 3／MIUIX／COUI | 均通过现有设置切换并保留数据；页面信息结构一致。COUI 操作可纵向排列，MIUIX 大字号导航文字会换行。 |
| 320dp、字体 1.5 | 三风格分别检查首页、趋势和操作区域。修复 COUI 账号挤压后，未见文字竖排、按钮重叠或内容画在底栏后面；长页面需滚动。不是全部页面的大字号验收。 |
| 640dp×360dp 横向、字体 1.0 | 三风格课程／电量并排，底栏与正文视口分离；低高度需滚动，不将未滚入视口的内容判定为丢失。 |
| 无开学日期 | 当前模拟器真实课表未配置日期，仅显示设置提示及同步操作，不同时显示“今天没有课程”。未改用户日期来制造其他状态。 |
| 断网 | 关闭雷电 WLAN 同时中断 ADB，未取得有效断网验收截图；通过正常重启及系统网络界面恢复 WLAN，确认 WLAN、移动数据回到原启用状态。此项未通过运行验证。 |

截图、界面树和构建日志位于本机验收目录，包含账号与宿舍信息，仅保留本机，不纳入仓库。
测试后恢复模拟器原 1440×2560、字体 1.0、MIUIX＋跟随系统，WLAN／移动数据均为启用，App 留在首页。未清除 Cookie、账号、课表、宿舍或历史记录。

### 仍需验证

- 重新登录后的教学周和最近考试实时成功路径、真实课程导入、验证码与跨服务 WebView 会话。
- 未绑定宿舍、无电量记录／仅单条记录、离线失败运行场景：已实现状态分支，但没有清除用户绑定和历史来制造场景，不能表述为模拟器已全部实测。
- 已配置开学日期的当前／下一课展示有逻辑测试依据，本轮未修改真实课表日期进行截图验收。
- 实体机厂商安全区、TalkBack 完整操作及其他字号／屏幕组合；此次模拟器检查不替代这些验证。

## 2026-11-07：课程模块 UI、设置归属与同步流程

### 实现范围

- `WeeklyScheduleScreen.kt`、`TodayScheduleScreen.kt`、`TodayScheduleViewModel.kt`：同一课程入口支持日／周模式，保留原周分页、课表选择、编辑和拖动。日模式按所选日期读取原课程及有效作息，可选日期、前后一天及回到今天。
- `CourseToolsScreen.kt`、`StyleSettingsComponents.kt`、`StyleSettingsViewModel.kt`、`StyleSettingsRepository.kt`：日期／总周数、节次／作息、显示／颜色归课表设置；学期仍在同步适配器内确认，账号仍归账号管理。
- `CourseDisplayName.kt`、`CourseBlock.kt`、`CourseDetailBottomSheet.kt`：不改写课程数据，展示时识别原序号后缀，详情单列序号；教师、地点、周次、节次、真实学分与备注沿用原字段。
- `ScheduleGridStyle.kt`、`ScheduleGridStyleComposed.kt`、`schedule_style.proto`：追加序号显隐偏好；旧配置默认保持显示。没有修改数据库或脚本课程接口。
- `XatuCourseSyncContext.android.kt`、`XatuLoginScreen.kt`：复用原账号和教务 repository，显示账号、当前学期、加载／确认失败，提供账号管理入口。
- `CourseImportProgress.kt`、`WebBridgeHandler.kt`、`WebViewScreen.kt`：目标课表、实际学期选择、进行中、真实失败及完成反馈。仅保存课程成功且通知完成才显示成功；跟踪原异步脚本返回以结束提前失败状态，不修改脚本文件。
- `App.kt`：通过更新状态向保留的导航条目传递底栏高度；课程页避让底栏并裁剪网格。宽屏操作合并为一行，表头随系统字号增加空间。课程模块继续共用三风格组件、原数据源和导航，不新增 Cookie 清理或 WebView 重建。
- `CourseModuleTest.kt` 与三份现有工程文档。上述路径可能同时包含前轮修改，未重置或整理继承的工作区。

### 自动化结果

```text
gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain
git diff --check
```

- 最终联合构建通过；59 项 host tests，0 失败、错误或跳过。新增 4 项覆盖日期未知／学期外状态、序号展示不改原名、偏好往返及旧值、真实保存与完成条件。
- Debug lint：0 error、310 warning；未进行全仓警告清理。
- 在 Node 中执行实际包装模板，验证等待异步任务、传递真实拒绝／运行错误、处理脚本内部 catch 后的返回及 ID 编码。测试脚本仅在临时目录，不入业务仓库。

### 模拟器观察与限制

在 API 34 x86_64 雷电模拟器覆盖安装 Debug，未卸载或清除数据，未安装实体设备。

| 检查 | 结果与边界 |
|---|---|
| 日／周及日期选择 | 前后一天、日期弹窗开关、日→周→日保留所选日期；原课表选择及课表设置可打开。 |
| 无开学日期 | 当前真实配置未设置日期，只提示设置，不同时显示“今天没有课程”。正常、未开始和学期外状态有逻辑测试；未修改用户日期制造课程截图。 |
| 同步失败 | 执行现有适配器，真实返回 `Failed to fetch`，进行中结束、错误持续可见、可重试；没有写入课程。网页当时报认证主机明文阻断，教务 repository 报会话失效，不计为实时导入验收通过。 |
| 三种风格 | 通过现有设置分别进入同一日／周页面；入口与无日期状态一致。没有真实课程数据时，不能据此宣称全部课程字段已运行验收。 |
| 窄屏／大字号／横屏 | 检查 320dp、1.5 倍字体及 640dp×360dp 横向布局，修正按钮挤压、表头裁切与首次底栏边距。低高度下内容可滚动，日期弹窗可关闭。完整 TalkBack、实体机及其他组合仍待验证。 |
| 连续配置切换 | COUI 下连续改变系统字号和模拟器分辨率曾触发一次 192MB Java 堆内存不足。已保存崩溃日志，并将新顶部宽度判断改为窗口尺寸读取，移除额外子组合；尚未确认根因或证明内存问题消除，不能标记此项通过。 |

真实同步成功、重新登录后当前学期确认、带数据的详情与拖动、验证码及跨服务会话仍需真实账号／网络环境验收。没有用假课程补齐，也未清除账号、Cookie、课表、宿舍或电费记录。模拟器最终恢复原分辨率、字体及 MIUIX／跟随系统。

截图、界面树、包装模板验证及构建日志仅在本机验收目录 和相邻日志中；可能含账号信息，不加入仓库。未提交、推送或发布。


## 2026-11-07：设置体系重组验收

执行 `gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain`：最后一版构建成功，60 项 host tests 全部通过（无失败、错误或跳过），Debug lint 0 error、310 warning。导航测试增加设置分类/宿舍配置返回路径及新路由序列化恢复。`git diff --check` 通过；未清理无关警告。

雷电 Android 14 模拟器覆盖安装 Debug x86_64，在 1440×2560、360dp 可用宽度下检查：
- 六类设置默认字号可见；课程日期、教务查询分类、校园配置、宿舍选择、签到位置/权限、账号与隐私、系统/网络页可进入并返回。
- Material 3、MIUIX、COUI 切换后设置分类相同，偏好保存反馈正常。账号信息、当前课表与已保存宿舍仍显示；没有执行清理、解绑或重新导入。
- COUI 系统字号 1.6 下账号信息可读，设置列表可滚动；日期/周次值纵向排列。复现并修复顶栏被列表式返回按钮撑大的问题，最终截图确认标题和图标正常。
- 打开退出确认并取消，未执行实际退出。测试结束恢复 font_scale=1.0、MIUIX 和原“跟随系统”模式。

截图与 UI XML 仅在本机验收目录，含账号/宿舍信息的证据不入库。菜单/源码检查不等于教务请求或登录会话实时验收：本轮未重新验证校园网、验证码、真实余额刷新、实际换账号/退出/清数据与定位授权。未新增多账号存储、应用内字体/密度配置，学期仍在原同步/查询流程选择。未修改 API、数据模型、存储格式或 WebView 生命周期；保留全部继承工作区改动，未提交或推送。

## 2026-11-07：校园服务与电费重构验收

### 修改范围

- `XatuCampusService.kt`、新增 `XatuElectricityResponse.kt`：统一电量转换、缴费平台失效分类、空电量与真实错误；沿用原接口、参数和 Cookie 来源，不关闭 TLS 校验。
- 新增 `XatuElectricityRepository.kt`：首页与详情共用查询反馈，持久化仍由原校园 repository 完成；不新增后台轮询或存储格式。
- 新增 `XatuElectricityPage.android.kt`，更新 `XatuServicePage.android.kt`、`XatuHomeViewModel.android.kt`、`XatuHomeScreen.android.kt`、`XatuPortalScreens.android.kt`：电量、时间、缓存来源、真实趋势和记录、逐级房间草稿与显式保存、校园服务分类。修复快照未读取就开始恢复房间的问题。
- 新增 `XatuElectricityTest.kt`，更新现有三份工程文档。上述文件部分同时含继承改动，未重置或清理工作区。

### 自动化

```text
gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain
git diff --check
```

联合构建成功，63 项 host tests、0 失败/错误；新增三项覆盖电量计算/负值/舍入/非法返回值、登录错误码与空记录区分、失败/刷新/时间或房间不一致时的缓存标注。Debug lint：0 error、310 warning，未清理无关警告。没有重复运行未涉及的 Release 构建。

### 雷电 API 34 模拟器实际检查

- 覆盖安装 Debug x86_64，保留账号、课表、Cookie、宿舍和电量历史。2026-11-07 18:06、18:12 的真实刷新成功，查询时间及本机历史更新；首页与详情使用同一结果。真实接口电量发生变化，未用缓存或假值代替请求成功证据。
- 真实读取项目、校区、楼栋和房间选项；启动后自动恢复原房间。选择另一个房间仅作草稿，返回后仍保留原房间与历史；未点击保存、解绑或清数据。
- 通过临时不可达代理制造真实连接失败：显示失败原因、保留旧电量和原查询时间，并明确标记“本次查询未取得电量／上次查询缓存”。测试后恢复代理，真实刷新重新成功。重启后仅有缓存时标为非实时。
- Material 3、MIUIX、COUI 均能打开相同详情和房间设置。360dp 宽度、COUI 1.5 倍字体下检查电量、时间、换行按钮与逐级选择器；未见重叠，内容可滚动。未因此声称横屏、所有设备或完整 TalkBack 已验收。
- 真实趋势、最近记录及全部记录弹窗可查看。空历史/单记录分支有源码与自动化依据，但未清除当前真实记录制造空状态。

登录真实过期、重新登录后的缴费会话恢复、实际保存换房/解绑、跨账号隔离及实体设备仍未实测；会话错误分类通过测试，不代替真实账号结论。校园网页入口保留，不代表其校园网权限和网页业务均已验收。截图/UI XML 仅保存在本机验收目录，含宿舍信息的证据不入库。未提交、推送或发布。

## 2026-11-07：核心页面视觉一致性验收

### 修改范围与不变边界

- 新增 `ui/components/UiPageLayout.kt`，调整 `UiStyleComponents.android.kt`、`UiFormComponents.android.kt`、`UiSelectionComponents.kt` 和 `NavigationComponents.kt`：尺寸、排版、按钮、可换行列表、轻分隔线、状态反馈、导航与滚动弹窗。
- Android XATU 页面：`XatuPortalScreens`、`XatuLoginScreen`、`XatuHomeScreen`、`XatuServicePage`、`XatuElectricityPage`、`XatuBalanceTrend`、`XatuNetworkNotice`。覆盖首页、教务/校园目录、账号、成绩、考试、学业、校历、签到及电费详情的共享容器与组件。
- 通用页面：`SettingsHomeScreen`、`SettingsScreen`、`CourseToolsScreen`、`ThemeSettingsScreen`、`MoreOptionsScreen`、`TodayScheduleScreen`、`WeeklyScheduleScreen`、`CourseDetailBottomSheet`；通知的 `GeneralSettingsCard` 移除重复适配说明。
- 本轮没有改动接口、Repository、数据模型、持久化偏好、WebView/Cookie 生命周期或依赖。主题分支集中在视觉组件；课程详情仅按风格重新测量高度，不切换字段或业务规则。保留继承工作区修改，未提交或推送。

### 验证命令和结果

```text
gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain
git diff --check
```

最终源码联合构建通过（日志仅保存在本机）；63 项 host tests、0 失败/错误，Debug lint 0 error、310 warning。没有为了视觉调整加入重复业务测试，也未宣称 host tests 能替代渲染验收。

### 模拟器观察

- API 34 雷电覆盖安装 Debug，保留原数据。Material 3、MIUIX、COUI 的账号页均检查默认字体和 1.5 倍字体；320dp 窄屏下学号与账号状态可见，按钮没有挤占账号文字。
- 进入首页、日/周课程、教务、校园、设置、账号及电费/房间页；成绩的缓存汇总和学期选择弹窗正常显示，关闭弹窗返回原页面。保留真实登录失效提示，没有把缓存当实时教务验收。
- 640dp×360dp 横向检查课程页及电费页；1.5 倍字号下课程页可通过滚动折叠顶栏，前后日期和日期选择操作可见。电费文字、刷新和换房操作可滚动查看，没有为横屏缩小系统字号。
- Material 3 深色模式检查设置、日课表、电费与房间设置，文字与背景可辨。导航 UI XML 中当前设置标签的父节点带 `selected=true`；没有将 UI XML 检查称为完整 TalkBack 验收。
- 核对主题切换后账号、原房间、历史记录仍存在。测试结束恢复 1440×2560、font_scale=1.0、MIUIX、跟随系统；未保存测试宿舍、退出账号或清数据。

### 验证限制

当前课表没有可用于运行验收的课程，课程详情的长名称/教师/地点布局仅完成源码修正与编译，不能声称带真实数据已验收。真实教务会话在本轮前已过期，实时成绩/考试、验证码和主题切换前后有效 WebView 会话连续性仍需重新登录验证。尚未逐一运行深层课程编辑、小组件配置、所有机型/字号组合和完整 TalkBack；这些页面继续共享宿主主题，不宣称每个控件均为 MIUIX/COUI 原生组件。

截图和 UI XML 仅保存在本机验收目录，含学号和宿舍信息的证据不入库。未安装实体设备、提交、推送或发布。

## 2026-11-07：重构后完整用户任务验收

### 环境与结论边界

本轮在雷电 Android 14 / API 34 模拟器 `emulator-5554` 实际操作当前已安装 Debug 2.1.0。开始重新检查 main 分支、AGENTS.md 和工作区，保留全部 152 条继承修改。本轮仅补充本报告，没有修改业务源码、依赖或配置，也没有提交、推送、发布或安装新包。没有连接实体设备。

教务会话已经失效，当前课表未设置开学日期且没有可供详情验收的课程。历史成绩、考试、学业页展示的是已有缓存，不能据此判定实时教务功能通过。电费使用现有缴费会话，本轮点击刷新成功，查询时间更新到 18:47；此结论不代表教务登录有效。

### 22 条任务结果

| 序号 | 用户任务 | 本轮实际操作与结果 | 验收结论 |
| --- | --- | --- | --- |
| 1 | 首页当前教学周 | 首页显示“教学周尚未确认”及登录失效，没有伪造周次 | 状态展示通过；实时周次受登录阻塞 |
| 2 | 今日课程 | 课程→日视图，前后日期/日期入口可见；仅提示设置开学日期，没有同时断言今天无课 | 未配置状态通过；真实课程未验收 |
| 3 | 本周课程 | 日/周切换可用，显示日期、节次和作息网格；本地没有课程 | 页面操作通过；真实课程和周次切换未完成 |
| 4 | 课程页同步 | 课程→同步课程表→XATU 树维适配器，可找到适配器；认证跳转被明文策略拦截 | 不通过，未完成同步及成功反馈验收 |
| 5 | 切换学期 | 成绩页“全部历史”→当前学期→全部历史，选择及对应缓存内容变化，恢复原选择 | 查询选择操作通过；同步中的学期确认受第 4 项阻塞 |
| 6 | 设置开学日期 | 课表设置→日期、周次与显示→设置开学日期，选择器打开后取消 | 仅入口/取消通过；未写入未经用户确认的日期，保存与课程联动未验收 |
| 7 | 课程详情 | 当前课表没有可点击课程 | 未完成，不造课程数据 |
| 8 | 成绩/学分/绩点 | 可查看历史缓存汇总，学分和绩点显示正常；登录失败提示存在 | 缓存展示通过；实时查询未通过 |
| 9 | 考试安排 | 展示已有缓存的课程及考场字段，明确提示登录失效 | 缓存展示通过；实时日期、时间、座位号未完成验收 |
| 10 | 学业进度 | 展示缓存的已修/要求学分、专业和 GPA，明确提示登录失效 | 缓存展示通过；实时进度及培养方案未完成验收 |
| 11 | 校历 | 进入后显示“正文未找到原图”错误及来源网页/刷新入口 | 未完成，需校园网环境验证；不将该失败认定为解析缺陷 |
| 12 | 首页电费摘要 | 首页直接显示电量、单位、查询时间与真实历史趋势；详情刷新后首页时间一致 | 通过，本轮有真实刷新 |
| 13 | 电费详情及单位 | 校园→宿舍电费，可见校区/楼栋/房间、电量和查询时间，单位为“度” | 通过，本轮有真实刷新 |
| 14 | 切换电费房间 | 切换房间→房间选择器→返回，原绑定及历史仍在 | 选项/取消通过；未保存其他房间，避免清除真实本机历史 |
| 15 | 历史趋势 | 滚动查看真实查询曲线和最近记录，标明是本机查询而非逐日账单 | 已有历史展示通过；空历史分支未重新制造 |
| 16 | 进入签到 | 校园→校园签到，实际加载统一认证网页和账号/验证码登录选项 | 入口和认证页通过；登录后签到、验证码挑战和定位授权未完成 |
| 17 | 切换账号 | 账号页明确当前只保存一个账号，须先退出再登录 | 未完成，没有第二个真实账号，不称为多账号切换已通过 |
| 18 | 退出登录 | 打开清除范围确认框并取消，原数据保留 | 确认/取消通过；未执行清 Cookie、宿舍及历史的实际退出 |
| 19 | 三风格切换 | Material 3→MIUIX→COUI，当前外观页不退出，账号/原房间/历史仍在；重启后 COUI 和深色模式仍保留 | 切换与偏好恢复通过；有效登录会话连续性仍未验证 |
| 20 | 全局设置 | 外观中切深色/跟随系统即时生效，系统设置可进入并返回，课表设置独立分类 | 已操作项通过；未把所有深层配置算作已验收 |
| 21 | 课程表设置 | 开启“显示周末”，离开后重新进入仍开启，再恢复原关闭值；核对 switch checked 状态 | 该设置保存/恢复通过；日期保存另见第 6 项 |
| 22 | 返回首页 | 普通原生详情返回原目录，再通过主导航返回首页成功 | 原生路径通过；同步 WebView 返回存在阻断，见下 |

### 确认的交互问题与最小修补顺序

1. **P0：同步认证被 Android 明文策略阻断。** 实际 WebView 地址为 `http://authserver.xatu.edu.cn/authserver/login?service=...`，显示 `net::ERR_CLEARTEXT_NOT_PERMITTED`。`androidApp/src/main/res/xml/network_security_config.xml:3-10` 只允许教务 HTTP 主机，没有允许认证主机；Manifest 第 21 行引用该配置。不能把此问题写成“需要校园网”而忽略客户端阻断。下一轮先验证认证 HTTPS 与重定向链，再选择修正入口或最小域名许可，不全局放开明文或跳过 TLS。
2. **P1：同步失败页不能可靠退出。** 错误页系统返回、工具栏返回以及重复返回后仍回到相同错误页；最终通过 force-stop 后重开 App 才继续测试，此过程不算正常返回成功。`WebViewScreen.kt:185-195` 在有网页历史时优先 goBack，Android 控制器 `WebView.android.kt:129-139` 同样依赖 canGoBack。下一轮为原生返回/关闭建立明确出口并回归重定向失败历史，不能通过清 Cookie 或销毁日常会话解决。
3. **P1：账号连接状态与实际会话矛盾。** 首页/成绩页已提示失效，账号页仍写“教务已连接”。`XatuLoginScreen.android.kt:50` 仅根据保存的 loggedIn 布尔值决定文案。下一轮复用真实会话失效状态，或将本机保存状态明确命名，避免将已保存账号表述为在线连接。
4. **P2：失败与空数据混淆。** 在无成绩缓存的学期，页面同时显示“教务登录已失效”与“暂无成绩”。`XatuServicePage.android.kt:413` 仅根据列表空判定；考试第 449 行有同类判断，但本轮未在无缓存考试上实测。下一轮区分查询失败、没有可展示缓存和成功空结果，保留真实缓存及错误原因。

本轮为验收记录，没有新增业务修复。确认上轮已修效果：未设日期不再同时显示今天无课；COUI 账号信息不再被操作按钮挤没；课程设置独立于全局设置；电量摘要与详情均使用“度”。这些结论不覆盖仍被阻塞的实时业务。

### 布局、主题与验证证据

- 本轮实际截图检查 Material 3 默认首页、电费趋势；COUI 默认/深色首页及账号、320dp 窄屏 + 1.5 倍字体账号；640dp×360dp + 1.5 倍字体横向首页。已查看区域未发现账号裁切、文字按钮重叠，横向首屏内容较少，需滚动；不宣称所有页面与全部组合都通过。
- 三种风格切换仍使用相同业务入口；账号、电量与历史没有被换肤清除。有效会话已经失效，因此不能据此声称登录 Cookie 连续性端到端通过。
- 复用上一轮输入未变的 `:androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:lintDebug --offline --console=plain` 成功证据。本轮读取现有 XML 核对 63 项 host tests、0 失败/错误/跳过，lint 报告 0 error、310 warning。没有修改源码后重复构建，也没有新跑 Release 并冒称已执行。
- 本轮 `git diff --check` 通过，仅提示已有文件的 LF/CRLF 转换警告。最终仅追加本报告；原有未提交内容保持。
- 截图和 UI XML 留在本机验收目录，含账号/宿舍信息，不入库、不公开。文件名不能替代截图内容判断；`62-wide-large-home` 实际仍是失败 WebView，真正横向首页证据为 `68-home-wide`。

### 尚未完成的真实环境验证

- **模拟器**：已实际执行上述路径；没有把入口打开、缓存读取或构建成功计为完整业务成功。
- **实体设备**：未连接，没有安装或实测。
- **真实账号**：存在原账号缓存和可用缴费会话；教务会话失效，没有重新认证，不能验收实时成绩/考试/学业/教学周、真实课程导入、有效会话换肤连续性。
- **校园网**：当前网络没有校园网权限证明；校历原图/OCR 等需校园网复测，不据失败推断代码缺陷。
- **定位权限**：检查到粗略/精确定位均未授权；未申请权限或提交签到，不声称权限拒绝/允许、实地定位和签到通过。
- **破坏性数据操作**：换房保存会清除原房间历史，退出会清除 Cookie 和校园本机数据。本轮均只检查确认/取消，需使用可重新登录的测试账号和可恢复的房间配置补测。

测试结束恢复本轮初始 Material 3、跟随系统、font_scale=1.0、wm size reset，恢复“显示周末”为关闭，并回到首页；未更改开学日期或真实房间绑定。下一轮只需先修以上四项，再在有效教务登录下补齐同步、日期保存、课程详情与实时教务任务，最后补校园网、定位及实体设备验收。
