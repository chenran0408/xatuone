# 一校通工程基础审计

审计日期：2026-11-07  
审计基线：`main`，`HEAD=324aea1`（与 `origin/main` 相同）  
审计方式：源码、仓库文档、Gradle 配置、工作流与测试目录静态检查；未修改源码。仅新增本报告。

## Git 与推送状态

- 当前分支为 `main`，远端为 `origin`（`chenran0408/xatuone`）。
- `git rev-list --left-right --count origin/main...HEAD` 输出 `0 0`；本次执行 `git push origin main`，结果为 `Everything up-to-date`。
- 工作区存在多项未提交修改，开始审计前已存在，本次没有暂存、覆盖、提交、清理或重置它们。它们没有被本次 push 推送。
- `docs/engineering-audit.md`、`docs/architecture.md` 和若干源码文件原本已经有未提交修改；本报告单独新增，未覆盖这些文件。

## 架构概况

### 技术栈与构建

- Kotlin Multiplatform，Kotlin `2.4.0`、Compose Multiplatform `1.11.1`、AGP `9.3.1`、Gradle Wrapper `9.7.1`，Android/JVM toolchain 21，Android compile/target SDK 37。版本定义见 `gradle/libs.versions.toml`，目标见 `shared/build.gradle.kts` 与 `androidApp/build.gradle.kts`。
- UI 使用 Compose Multiplatform、Material 3、Navigation 3、自适应 Navigation Suite；Android 端另有 MIUIX 依赖。
- DI 使用 Koin 注解与组件扫描；数据持久化使用 Room 3、DataStore 和文件缓存；网络使用 Ktor、OkHttp 与 Android WebView；校历 OCR 使用 ML Kit。
- Gradle 子项目是 `:shared`、`:androidApp`、`:desktopApp`。`iosApp` 是独立 Xcode/SwiftUI 工程，通过 `Shared` framework 启动 Compose 页面，不是 Gradle 子项目。

### 模块与平台边界

- `shared/src/commonMain` 同时容纳跨平台课程模型、数据库、repository、ViewModel、导航和主要 Compose 页面。
- `shared/src/androidMain` 提供 Android 数据库/平台能力，以及 XATU WebView、学术和校园服务网络实现、Android KeyStore 加密实现、定位/OCR。
- `shared/src/jvmMain` 提供桌面文件系统、数据库和平台服务实现；`shared/src/iosMain` 提供 iOS 数据库、网络、主题等实现。
- `androidApp` 负责 Android Application、Manifest、通知、后台 Worker 和原生课表小组件；`desktopApp` 是 Compose Desktop 壳；`iosApp` 是 SwiftUI 壳。
- XATU 的 service、academic repository 和 UI 实现基本集中在 Android source set；但账号和校园快照 repository/model 放在 `commonMain`，这使 Android 业务数据类型进入所有共享 target 的可见源码。

### 页面、导航与数据流

- 应用入口为 `shared/src/commonMain/.../App.kt`：设置 ViewModel 加载后创建 Navigation 3 back stack；`ScreenContent` 将 destination 映射至页面。
- 一级导航由 `AdaptiveNavigationScaffold` 提供：校园门户、今日课表、周课表、设置；窄屏用底部导航，宽屏用 Navigation Rail。
- 常见数据流是 Compose 页面收集 `StateFlow`，ViewModel 组合 repository Flow 并以 `viewModelScope` 执行业务；repository 负责 Room/DataStore/文件缓存；service 执行网络访问和解析；Koin 在平台及共享模块注册依赖。
- 课程、作息和课表配置使用 Room；应用/账号/校园快照设置使用 DataStore；学术成绩、考试、学业方案使用 Android 私有文件缓存并通过 Android KeyStore AES/GCM 加密；WebView 登录使用 Android CookieManager 持久会话。
- XATU 学术 ViewModel 会取消前一请求再启动新请求；普通 WebView 切页保持 WebView controller 会话，登录退出/切换路径清理 Cookie。

## 风险与问题

按影响与修复优先级排序。P0 为应先阻断/确认的安全或构建问题，P1 为高优先级可靠性/隐私问题，P2 为持续维护与质量问题。

### 已确认

1. **P1：XATU 个人信息进入普通 DataStore，并处于 Android 自动备份范围。** `XatuCampusSnapshot` 包含宿舍楼/房间、电费余额历史和签到坐标，`XatuCampusRepository` 将整个快照 JSON 写入 Preferences DataStore；`XatuAccountRepository` 存储学号。对应实现见 `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/data/repository/XatuCampusRepository.kt:14-58,65-95`、`XatuAccountRepository.kt:13-47`、`data/di/DataStoreModule.kt:55-72`。Manifest 设置 `allowBackup=true`，而 `backup_rules.xml` 和 `data_extraction_rules.xml` 没有有效排除规则，后者仍保留 TODO 注释（`androidApp/src/main/AndroidManifest.xml:19-22`、`androidApp/src/main/res/xml/backup_rules.xml:8-13`、`data_extraction_rules.xml:6-19`）。相比之下学术缓存使用 KeyStore 加密（`XatuAcademicRepository.kt:100-105`、`SecureCrypto.android.kt:12-22`）。影响：备份行为可能保存学号、宿舍和坐标等信息；恢复到另一设备时，KeyStore 密钥不可随文件恢复，学术密文缓存也需要专门处理恢复失败。云备份实际是否启用由设备/用户设置决定，WebView cookie 目录是否被系统备份需设备验证。

2. **P1：XATU 的 `expect` API 只看到 Android `actual`，其他共享 target 缺少对应实现；跨平台构建没有完成验证。** 公共 expect 声明位于 `shared/src/commonMain/.../ui/settings/xatu/XatuLoginScreen.kt:227-253`，实现只在 `shared/src/androidMain/.../XatuLoginScreen.android.kt:67-80` 与 `XatuServicePage.android.kt:67`。共享模块还声明了 JVM、iOS Arm64 和 iOS Simulator targets（`shared/build.gradle.kts:18-29`），desktop 与 iOS 都会使用共享 `App` 导航。此次尝试 `:shared:compileKotlinJvm --offline` 在依赖解析阶段失败，因为本机缓存缺少 Desktop/JVM Compose、Navigation、DataStore 等依赖，未进入 Kotlin 编译，因此这里记录为**源集覆盖缺口与高优先级待验证风险**，不声称目标编译已经失败。CI 构建桌面包，但没有 iOS 构建任务。

3. **P2：CI 资源输入随上游分支移动，提交构建不可完全复现。** `.github/workflows/ci-build.yml:34` 从 `shiguang_warehouse` 的 `index-pb-release` 克隆最新索引，`53` 从 `main` 克隆最新 resources；没有固定 commit SHA 或资源版本。相同一校通提交的学校索引/适配资源可能随上游分支变化，CI 产物不再只由本仓库提交决定。

4. **P2：自动化测试集中在 7 个 XATU host parser/坐标用例，没有 UI、repository/数据库、账号生命周期或 WebView 集成测试。** 目前仓库仅找到 `shared/src/androidHostTest/.../XatuCoreTest.kt`（7 个测试）；根本模块有 `commonTest` 依赖，但没有 commonTest 源码。CI 运行 Android compile、host tests、Release lint 和 Release build（`.github/workflows/ci-build.yml:138-147`），不运行 iOS target 编译或端到端流程。由此不能自动保障会话恢复、账号切换清理、数据库迁移、导航和页面状态交互。

5. **P2：本地 JVM/桌面 target 缺少离线依赖缓存，当前机器无法验证其编译。** `:shared:compileKotlinJvm --offline` 因多项 Desktop/JVM artifacts 未缓存而在依赖解析阶段终止；这不是源代码编译错误证据。CI 有 Windows/Linux/macOS 桌面包矩阵（`ci-build.yml:156-178`），建议以后在受支持的在线 CI 结果中明确记录桌面构建状态。

### 需要进一步验证的风险

- **CI 发布签名环境是否受保护。** `build-android` 同时响应 PR 和 push，并声明 `environment: Release-Signing`（`ci-build.yml:7-10,94-98`）；同一 job 后续读取签名 secrets（`134-147`）。仓库文件没有声明该 Environment 的审批人、分支限制或 secrets 策略，也未显式设定 job `permissions`。需要在 GitHub 仓库设置中确认 `Release-Signing` 的保护规则，并确认 PR 构建不向未受信任代码暴露签名 secrets。
- **Android 备份/换机恢复行为。** 需要设备验证 cloud backup 是否包含 XATU DataStore、WebView cookie 和 KeyStore 加密缓存；恢复后学术缓存解密失败的用户体验也尚无测试。
- **iOS target 是否有意继续支持。** 仓库保留 Xcode 壳和 iOS framework targets，但 XATU `expect` 未见非 Android actual，CI 不构建 iOS。需确定这是 Android-only 产品的残留 target，还是仍要求跨平台可构建。
- **CI 当前实际桌面构建是否成功。** 本机 offline 尝试被依赖缓存阻断；静态审计不能替代 CI run 结果。
- **真实校园网服务端行为。** 成绩、考试、校历最新页面、电费和签到依赖登录会话或校园网权限；本审计未进行真实账号网络操作。现有验收记录也标明这些流程需现场验证。

## 当前没有发现需要改动、建议保留的设计

- 继续使用 `shared` 的 KMP source set 复用课程、Room、DataStore、导航和 UI；当前代码规模尚无证据说明必须增加 Gradle 子模块。
- Android 原生 WebView/Web Chrome 定制、CookieManager、定位权限、KeyStore 和 OCR 放在 Android source set，是符合平台职责的做法。
- Repository 持有缓存，ViewModel 持有页面状态，Compose 页面负责呈现和事件分发；新学术页面已使用 `XatuAcademicViewModel`，并取消切换中的旧请求。
- 课表主数据库与 Widget 数据库分离，利于桌面小组件读取轻量快照。
- 明文 HTTP 例外限定到 `jwgl2018.xatu.edu.cn`，其他域名由 base config 禁止 cleartext（`androidApp/src/main/res/xml/network_security_config.xml:3-10`）。该例外由上游教务服务能力决定，目前不建议扩大。
- Android Release 开启 R8 和资源压缩，签名由 CI 参数注入；本地 Release 不依赖仓库内固定签名文件。

## 建议治理顺序

1. **P0：明确隐私与构建基线。** 决定 XATU 账号/校园快照/WebView 会话和 KeyStore 密文哪些允许进入系统备份；据此定义备份排除与换机行为。补齐并实际运行 Android、JVM/Desktop、iOS source-set 编译矩阵，先确认非 Android `expect` 覆盖是支持要求还是应收敛的平台范围。
2. **P1：固定构建输入与 secrets 边界。** CI 使用可追踪的上游资源版本/提交；检查 `Release-Signing` 环境保护、PR 权限和签名作业隔离。
3. **P1：为核心数据与生命周期补最小测试。** 优先覆盖备份/账号切换清理、学术缓存加密与旧数据迁移、DataStore 快照隔离、WebView 导入 Bridge 会话门控、关键请求取消。测试只覆盖可复现的风险，不做泛化测试堆量。
4. **P2：补平台 CI 与维护记录。** 为仍支持的 iOS/JVM targets 添加真实编译任务；记录上游资源版本、构建版本和发布产物输入，确保问题可追溯。
5. **按实际故障再处理性能和进一步模块化。** 当前未发现足够证据支持拆分 Gradle 模块或重写现有数据流。

## 检查范围与限制

- 检查了：Git 分支/远端/提交/工作区；`AGENTS.md`、`README.md`、`docs/architecture.md`、`docs/engineering-audit.md`、`docs/repository-migration.md`、功能验收和 UI/UX 记录；Gradle settings/build scripts/version catalog；Android Manifest、网络安全和备份规则；核心导航、DI、DataStore/Room repository、WebView、XATU service/repository/ViewModel；现有 tests 和 CI workflows。
- 本轮未检查每一个设置/编辑页面的业务细节、第三方依赖完整传递树、线上 GitHub Environment 设置、签名 secrets 配置、Android 系统备份实测、真实校园网 API、完整 iOS 编译或所有设备尺寸 UI。
- `codebase-memory` MCP 项目索引连接失败（`Transport closed`），结构信息使用源码检索和 Gradle 配置核实。
- Android host tests 和 Android Release 相关任务此前有通过记录；本次只额外尝试 JVM shared 编译，因离线缺少依赖而在解析阶段停止。未将这次依赖失败当作源代码失败。
