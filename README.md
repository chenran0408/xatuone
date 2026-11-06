# 一校通（XATU One）

一校通是面向西安工业大学（XATU）的校园一站式服务应用。项目保留课程表、提醒、导入导出等基础能力，并在 Android 端提供教务、成绩、学分、考试、校历、电费和校园服务入口。

## 工程结构

- `shared`：Android 应用内部复用的课程表能力、数据层、导航和 Compose UI。
- `shared/src/androidMain/.../xatu`：XATU 学术和校园服务模块。
- `androidApp`：Android 入口、WebView 会话、通知和桌面小组件。
- `docs`：架构约定和工程审计记录。

项目只面向 Android 手机；不包含 iOS、macOS、Windows 或 Linux 应用。

XATU 页面复用 Android WebView 的持久化会话。Cookie 在用户退出登录或切换学号时清理，普通页面切换会保留会话。

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
