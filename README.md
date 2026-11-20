# 一校通（XATU One）

一校通是面向西安工业大学（XATU）的校园一站式服务应用。项目保留课程表、提醒、导入导出等基础能力，并在 Android 端提供教务、成绩、学分、考试、校历、电费和校园服务入口。

本分支提供 iPhone 预览版，支持 iOS 16 及以上版本。下载 IPA 后使用自己的 Apple 账号签名安装。[iPhone 下载与安装说明](iosApp/README.md)

## 工程结构

- `shared`：Android 应用内部复用的课程表能力、数据层、导航和 Compose UI。
- `shared/src/androidMain/.../xatu`：XATU 学术和校园服务模块。
- `androidApp`：Android 入口、WebView 会话、通知和桌面小组件。
- `docs`：架构约定和工程审计记录。

- `iosApp`：iPhone 版的 SwiftUI 页面、教务与校园服务、测试及 IPA 构建脚本。

XATU 页面复用 Android WebView 的持久化会话。Cookie 在用户退出登录或切换学号时清理，普通页面切换会保留会话。

## 应用身份与图标

- 中文名称：一校通；英文名称：XATU One。Debug 与 Release 使用相同名称。
- Application ID：`com.xatuone`。源码命名空间、签名与已有存储键保持兼容，不随品牌更名。
- 标识源文件：`shared/src/commonMain/composeResources/drawable/ic_brand_mark.xml`。Android 构建自动生成同源资源，桌面自适应图标、单色主题图标、启动页、通知和关于页共用此标识。
- 图标背景为深青色；启动页另有浅色、深色背景。不维护重复的密度位图或单独的圆形图标。
- 新建 WebDAV 备份默认目录为 `XATU One`；已有远端目录及旧配置缺省路径继续沿用，避免更名后找不到备份。

## 构建

```text
./gradlew :shared:compileAndroidMain
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:lintRelease
./gradlew :androidApp:assembleRelease
```

当前仓库从拾光课程表代码库独立出来，后续校园服务和 UI 改动在本仓库维护；通用课程适配脚本仍沿用拾光适配协议。

## 开发约定

请先阅读 [AGENTS.md](AGENTS.md)、[架构说明](docs/architecture.md) 和 [工程审计](docs/engineering-audit.md)。新增校园服务时按领域目录组织模型、service、repository 和页面，并补充可运行的解析测试。
