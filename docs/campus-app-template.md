# 校园 App 模板边界

这份说明把当前 Android 工程中已经存在、且第二个校园 App 可以直接复用的部分与 XATU 专属实现分开。它不是新的多学校框架，也不要求把当前工程拆成更多 Gradle module。

## 模板目录

```text
campus-app/
├── androidApp/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                         # 应用名、图标、启动页、系统入口
│       └── kotlin/.../MainActivity.kt
├── shared/
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/.../App.kt        # 应用外壳和导航宿主
│       │   ├── kotlin/.../Navigation.kt
│       │   ├── kotlin/.../ui/
│       │   │   ├── components/           # 共享状态、表单、导航和页面布局
│       │   │   ├── schedule/             # 课程表 UI 和课程交互
│       │   │   ├── today/                # 今日课程
│       │   │   ├── theme/                # 主题入口和形状
│       │   │   └── settings/             # 通用设置和课程表设置
│       │   ├── kotlin/.../data/
│       │   │   ├── db/                   # 课程数据存储
│       │   │   ├── model/                # 通用设置、课程和导航模型
│       │   │   └── repository/           # 通用存储和课程仓库
│       │   └── proto/                    # 课程表显示配置
│       └── androidMain/
│           ├── kotlin/.../ui/components/ # 三套 Android 风格的实际组件
│           ├── kotlin/.../ui/theme/      # Material 3、MIUIX、COUI provider
│           └── kotlin/.../ui/schoolselection/web/
│                                            # 可选的 Android WebView 会话壳
└── docs/
    └── campus-app-template.md
```

模板仍保持当前工程的 Android-only 形态：`androidApp` 负责系统入口，`shared` 负责 Compose 页面、课程数据和 Android source set。不要为了模板目的添加 iOS、Desktop 或新的插件注册表。

## 可以复用的能力

| 能力 | 当前源码 | 复用结论 |
| --- | --- | --- |
| 应用外壳与导航 | `shared/src/commonMain/kotlin/.../App.kt`、`Navigation.kt` | 可复用。替换目的地列表和首页内容即可。 |
| 主题切换 | `ui/theme/Theme.kt`、`ui/theme/Theme.android.kt`、`ui/components/UiStyleComponents*`、`UiFormComponents*`、`UiSelectionComponents*`、`UiNativeLayouts*` | 可复用。三套主题共用业务状态；新项目只需保留实际使用的三套依赖和组件。 |
| 底部导航/侧栏 | `ui/components/NavigationComponents.kt`、`UiNativeLayouts.android.kt` | 可复用。导航项来自业务目的地，不把 XATU 服务写进组件。 |
| 首页服务入口外壳 | `UiPageLayout.kt`、`UiScaffold`、`UiServiceRow`、`UiCard` | 可复用布局和状态反馈；服务清单必须由新项目自己定义。 |
| 课程表 UI | `ui/schedule/`、`ui/today/`、`data/db/`、`CourseTableRepository` | 可复用。课程适配器、学期规则和导入脚本属于数据源层，不属于课程表 UI。 |
| 账号与 WebView 会话壳 | `ui/schoolselection/web/PlatformWebSpec.kt`、`WebViewScreen.kt`、Android `WebView.android.kt` | 可复用会话生命周期和 Cookie 持久化思路。移植时删除 XATU 签到定位桥接，只保留通用网页登录、返回、刷新和退出会话。 |
| 加载/空/错误/成功状态 | `UiFeedback`、`UiLoading`、`UiActionButton`、`UiPageLayout.kt` | 可直接复用。状态由页面真实数据流提供，不复制一份状态模型。 |
| 周期趋势图 | `XatuBalanceTrend.android.kt` | 当前实现绑定 `XatuBalanceRecord`，不能直接作为通用组件复制。第二个项目确实需要趋势数据时，再抽取“时间点 + 数值”的最小模型。 |
| 应用品牌资源流程 | `androidApp/build.gradle.kts` 的 `PrepareBrandResources`、`res/`、`composeResources/drawable/ic_brand_mark.xml` | 可复用构建流程和自适应图标结构；品牌文件必须替换。 |

## 必须留在 XATU 专属层

以下内容不能放进通用模板，也不能通过配置项伪装成通用能力：

```text
shared/src/androidMain/kotlin/.../xatu/
├── academic/                 # 成绩、考试、学分、学业进度、教学周
├── campus/                   # 校历、电费、签到、定位、校园接口
└── AndroidXatuSessionRepository.kt

shared/src/androidMain/kotlin/.../ui/settings/xatu/
├── XatuHomeScreen.android.kt
├── XatuServicePage.android.kt
├── XatuPortalScreens.android.kt
├── XatuAcademicViewModel.android.kt
└── XatuElectricityPage.android.kt

shared/assets/offline_repo/
├── index/school_index.pb
└── schools/resources/XATU/

androidApp/src/main/assets/licenses/XATUHelper-Android.txt
androidApp/src/main/res/xml/network_security_config.xml
```

还需要替换或删除所有 XATU 专属内容：

- `authserver.xatu.edu.cn`、`jwgl2018.xatu.edu.cn`、`ehall.xatu.edu.cn` 等网址；
- `XatuAcademicRepository`、`XatuCampusRepository` 及其模型、解析器和缓存文件；
- XATU 学校索引、适配脚本和公开校历地址；
- XATU 账号、宿舍、电费和签到权限存储；
- XATU 首页、教务和校园服务清单；
- `XATU One`、包名 `com.xatuone`、XATU WebDAV 路径和 ICS `PRODID`；
- XATUHelper 来源声明，仅在实际继续使用其代码时保留对应许可证文件。

## 普通模板默认不带的入口

模板首页和设置不显示以下维护入口：

- 贡献者列表；
- 教务适配仓库和自定义仓库；
- 课表文件管理；
- 系统日历调试入口；
- WebView 开发者工具；
- 学校适配更新日志；
- 仅供维护者使用的导入、转换和调试页面。

这些代码当前仍可能存在于一校通仓库，因为它们服务于现有课程表维护流程；复制模板时不把它们登记到导航和普通设置即可。

## 新校园 App 接入步骤

1. 复制 `androidApp`、`shared/commonMain` 中的应用外壳、课程表、主题、状态组件和通用存储代码。
2. 删除 `xatu/`、`ui/settings/xatu/`、XATU 离线资源和 XATU 网络安全域名配置。
3. 在 `Navigation.kt` 建立新学校实际需要的首页、课程、服务和设置目的地；不要保留 XATU 目的地名称。
4. 实现新学校自己的 `Service`、`Repository`、模型和账号会话，并让页面只观察 `StateFlow`。
5. 如果学校只提供网页服务，接入通用 WebView 会话壳；登录域名、退出逻辑和页面 URL 由新项目提供。
6. 将首页服务摘要接入真实 repository。没有真实数据的服务不创建原生入口。
7. 替换应用名称、英文品牌、包名、版本号、图标、启动页、通知名称、Widget 名称、分享文本和 ICS `PRODID`。
8. 替换主题颜色和品牌图形；Material 3、MIUIX、COUI 仍使用同一份业务状态和导航目的地。
9. 只有第二个项目确实有周期数据时，才从 `XatuBalanceTrend` 提取通用趋势图模型；否则保留当前实现，不增加抽象层。
10. 运行构建、host tests、lint、Release 构建，并在设备上验证登录会话、课程滚动、三套主题和系统返回行为。

## 最小构建命令

```text
set JAVA_HOME=<JDK 21>
gradlew.bat :shared:compileAndroidMain
gradlew.bat :shared:testAndroidHostTest
gradlew.bat :androidApp:lintDebug
gradlew.bat :androidApp:assembleDebug
gradlew.bat :androidApp:assembleRelease
```

当前工程使用 Android `minSdk 34`、`compileSdk 37`、`targetSdk 37`。新项目若降低 minSdk，需要重新验证 MIUIX、COUI、WebView 和主题专属导航组件，不能只修改一个版本号。

## 当前模板结论

能直接复用的是应用外壳、课程表、主题组件、导航容器、状态反馈、DataStore/Room 组织方式和 WebView 会话生命周期。XATU 的学术、电费、校历、签到、学校索引、服务 URL 和品牌资源不是模板能力。当前没有创建新的模板 module，也没有提交、推送或发布任何改动。
