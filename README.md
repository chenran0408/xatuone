# 一校通（XATU One）

一校通是面向西安工业大学（XATU）的校园一站式服务应用。项目保留课程表、提醒、导入导出等基础能力，并在 Android 端提供教务、成绩、学分、考试、校历、电费和校园服务入口。

## 工程结构

- `shared`：课程表通用能力、数据层、导航和 Compose UI。
- `shared/src/androidMain/.../xatu`：XATU 学术和校园服务模块。
- `androidApp`：Android 入口、WebView 会话、通知和桌面小组件。
- `docs`：架构约定和工程审计记录。

XATU 页面复用同一个 Android WebView 会话。Cookie 只在用户主动退出登录时清理，页面切换不会销毁会话。

## 构建

```text
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:lintRelease
./gradlew :androidApp:assembleRelease
```

当前仓库从拾光课程表代码库独立出来，后续校园服务和 UI 改动在本仓库维护；通用课程适配脚本仍沿用拾光适配协议。

## 开发约定

请先阅读 [AGENTS.md](AGENTS.md)、[架构说明](docs/architecture.md) 和 [工程审计](docs/engineering-audit.md)。新增校园服务时按领域目录组织模型、service、repository 和页面，并补充可运行的解析测试。
