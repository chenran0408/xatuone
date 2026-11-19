# XATUHelper 必要能力移植记录（2026-11-07）

参考源码：`Kelejun/XATUHelper-Android`，快照 `8fbbc1a8e11326f404d5d5477254a1feece4177c`。
当前工程：一校通 Android，保留现有业务模型、导航、DataStore、学术加密缓存和三风格共享组件。
本轮未复制参考工程、未新增第三方依赖、未保存密码，未提交、推送或安装。

## 本轮新增和补齐

下表路径省略包前缀 `com/xingheyuzhuan/shiguangschedule`；`A` 指 `shared/src/androidMain/kotlin`，`C` 指 `shared/src/commonMain/kotlin`。

| 能力 | 请求 / 平台 → 数据链路 | 页面、模型及本轮变化 |
| --- | --- | --- |
| 教务登录、验证码和动态验证 | CAS 登录地址 → Android 原生 WebView → `AndroidXatuSessionRepository.completeLogin()` → 教务首页教学周校验 → Cookie flush、`XatuAccountRepository` | 门户账号面板和 `WebViewScreen`；沿用真实网页登录，不模拟 CAS POST。保留 JS、DOM Storage、第三方 Cookie、iframe、脚本、资源和原生重定向。未取得教学周时不标记登录完成。 |
| Cookie 持久化和原生请求共享 | WebView `CookieManager` ↔ `XatuCookieJar` ↔ 现有 OkHttp → Academic/Campus Service | `A/data/di/XatuNetworkModule.android.kt`、`A/xatu/XatuCookieJar.kt`；请求按 URL 读 Cookie，响应 Cookie 连同属性写回同一存储并 flush，不另建 Cookie 库。 |
| WebView 会话复用、失败恢复和附件 | 门户真实服务 URL → singleton `AndroidWebViewController.shared` → `PlatformWebView` | 页面离开只 detach、flush 和释放页面回调，不销毁正常实例。渲染进程死亡后销毁失效实例，用户重载时恢复 URL，不清 Cookie。新增主框架 HTTP/网络失败提示及系统文件选择，取消时完成回调。 |
| 账号切换和退出 | 账号面板 → `XatuSessionRepository.clear()` → WebView 存储/Cookie、当前账号学术缓存、校园快照、账号状态清理 | 更换学号前需主动退出；重新登录同一学号不清会话。查询遇到过期登录仅报错，不自动清账号或缓存。课程表仍保留。 |
| 成绩、学分和绩点 | `teach/grade/course/person.action` → `person!search.action`，历史 `person!historyCourseGrade.action` → AcademicService/Parser → AcademicRepository → AcademicViewModel | `XatuGradePage`、`XatuGrade`、`XatuGradeSummary`；沿用单课及构成/汇总展示，新增课程/代码/学期本地搜索。先显示账号隔离缓存，再确认当前学期并请求；未知 HTML 抛错，避免覆盖为成功空结果。 |
| 考试安排 | `stdExamTable.action` → 考试批次 → `stdExamTable!examTable.action` → AcademicService/Repository/ViewModel | `XatuExam`，考试页保留日期/时间、课程代码、考场、座位和备注；按完整有效日期排序，展示今天/距考试天数。无年份不补猜；沿用首个非空批次结果规则，未改成未经验证的多批次合并。 |
| 学业进度和培养方案 | `myPlanCompl.action`、`myPlan.action` → AcademicService/Parser → AcademicRepository → AcademicViewModel | `XatuStudyPlan`、`XatuPlanSection`、`XatuCourseInfo`；新增课程检索、方案计划学期筛选。方案单独失败时保存已成功取得的进度、保留旧方案并明确提示部分失败。 |
| 校历原图和 OCR | 教务处校历索引 → 正文 → 原图 → 中文 MLKit OCR → `XatuCalendarParser` → `XatuCampusRepository.saveCalendar()` | `XatuCalendar`、`XatuCalendarSemester`，校历页沿用缓存原图、在线原图、识别文字和复制/来源入口；新增正文/原图选择、必要时底部二次识别。OCR 解码限制像素量；原图按内容哈希原子写入，避免先覆盖旧缓存所引用的文件。 |
| 电费查询和房间选择 | `jfpay.xatu.edu.cn/api/pay/project/getAllProjectList` → `pay/web/payEleCostController/` 的 `querySchoolList/queryBuildingList/queryRoomList/querySydl` → CampusService → CampusRepository | `XatuDormSelection`、`XatuBalanceRecord`；沿用项目/校区/楼栋/房间及绑定、刷新、解绑。原生响应 Cookie 现在回写 WebView；继续使用当前 Cookie 中的缴费 token。 |
| 电费历史趋势 | 真实查询成功 → 已有最多 400 条 `balanceHistory` → `summarizeXatuElectricity()` → 同一电费页 | 新增 UTC+8 按日投影，取当天末次观测；连续两日余额未增加时才计算减少量。历史弹窗支持按次/按日查看及有效日减少均值。既有近七次余额趋势保留。没有新接口和存储迁移，不是学校历史账单。 |
| 签到和定位权限 | 用户点击当前位置 → Android 前台定位和权限 → WGS84 坐标 → 用户确认保存 → CampusRepository → 签到网页 document-start JS | `XatuCheckinLocation`、`XatuCoordinate`；签到页新增单次当前位置、定位中/失败/精度反馈、WGS84/GCJ02选择。离页取消定位。仅签到页面可取自定义位置，文档开始脚本仅注入 `ehall.xatu.edu.cn`，不影响登录页。缺少提前注入能力时显示限制，不伪装启用成功。签到提交和结果仍在真实网页完成。 |
| 一站式服务与网络状态 | 门户服务 URL → 同一 WebView；系统 ConnectivityManager → 页面网络提示 | 保留一站式大厅、请销假、图书馆、校园网、充值、教务和一网通办网页入口。服务页明确显示断网/连接类型和校园网访问要求；不会把连上 Wi-Fi 等同取得校园网授权。 |

## 数据和生命周期约定

- 不改成绩、考试、课程、校园快照的持久化格式。日统计和检索是现有数据的投影。
- 学术缓存仍加密、按学号隔离；本轮改为临时文件写完再原子替换。
- 三种风格继续调用同一份页面和业务状态，不复制 Service、Repository 或业务页面。
- 不因换肤、返回或普通页面跳转清 Cookie。Android 进程/渲染进程死亡不能保留内存实例，恢复仍取同一 WebView 数据目录中的 Cookie；能否继续登录取决于学校服务端会话有效期。
- 本地学号仍来自用户输入，当前参考源码没有提供可直接移植且已确认的服务器学号核对链路。教学周成功只证明教务访问有效，**不证明本地学号与服务器身份相同**；不得称账号身份绑定已完全验收。

## 未移植或未扩展

- 参考工程的密码保存、模拟 CAS 提交、退出普通网页即销毁实例的做法，与本项目要求冲突，不采用。
- 未搬入参考工程的 Room 库、完整应用壳、主题/导航或其他平台代码。
- 未新增地图选点、班级课表、成绩变更通知、考试后台提醒、电费自动后台刷新及校园小组件。
- 未把校历推导的寒暑假自动写入正式课表/假期配置，也未新增 `coverageEnd` 存储字段；仍需核对原图和人工确认。
- 未建立多账号并行 Cookie 容器；切换为退出后重新登录，不会假称多账号免登录切换。
- 未接入签到结果原生查询，未把本机电费观测记录称作平台完整账单。

## 验证

使用已有 JDK 21、Android SDK 和 D 盘 Gradle 缓存，离线执行：

```text
gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :androidApp:lintRelease :androidApp:assembleRelease --offline --console=plain
git diff --check
```

编译和 40 项 host tests 已通过。新增 10 项移植测试覆盖 Cookie URL 读写与属性保留、未知 HTML 与明确空数据区分、真实考试批次失败不能被探测空结果掩盖、UTC+8 电费日统计及充值/缺日边界、WGS84保存、考试日期、校历正文/原图和跨年日期识别。
最终同一次验证中，Android 编译、40 项 host tests、Release lint 和启用 R8 的 Release 构建全部通过（`BUILD SUCCESSFUL`）；lint 为 0 错误、328 条警告。`git diff --check` 通过。未安装 APK。自动化结果不代表以下真实流程通过。

尚需验收：CAS 验证码/iframe/动态验证、服务间 SSO、冷启动 Cookie 恢复、账号切换及退出、真实附件上传、校园网校历/电费/各网页、OCR真实原图识别准确率、定位授权/拒绝/超时及签到首次定位、厂商 WebView和三风格实机交互。

参考实现的 MIT 许可及来源已随包保存在 `androidApp/src/main/assets/licenses/XATUHelper-Android.txt`，不改变本项目自身许可证。
