# 一校通开发约定

## 目录边界

- `shared/src/commonMain`：课程表、通用导航、共享数据和 UI。
- `shared/src/androidMain`：Android 专属的 XATU 服务、WebView、定位和 OCR。
- `androidApp`：Android 应用壳、权限、通知、小组件和发布构建。
- 主线构建和发布 Android 手机应用。独立分支 `ios/campus-preview` 的 SwiftUI 客户端与 XCTest 放在 `iosApp/`，使用同一份 XATU 课程适配脚本。iPhone 构建和自签步骤见 [iPhone 说明](iosApp/README.md)。

新增校园服务时，将网络解析放在 `shared/src/androidMain/.../xatu/<domain>`，持久化放在 repository，页面只负责状态展示和事件分发。

## 状态和数据

- 服务端数据由对应 repository 持有；页面使用单一状态流观察。
- 账号相关数据必须使用学号或账号键隔离。
- Cookie、成绩、考试、宿舍和定位属于敏感数据，不写入普通日志。
- 自定义签到位置桥接限制在 XATU 签到页面使用；通用课程导入写操作必须先由宿主启动导入会话，不能将此门控描述为 Web 来源白名单。

## 验证命令

```text
./gradlew :shared:compileAndroidMain
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:lintRelease
./gradlew :androidApp:assembleRelease
```

本地 Release 构建仅用于验证。正式签名必须通过 CI 的签名参数注入。
Android 正式版构建必须同时启用 R8 代码压缩（`isMinifyEnabled = true`）和资源缩减（`isShrinkResources = true`）。

## 修改流程

1. 先检查 `git status`，保护已有未提交修改。
2. 先改一个业务边界，再运行对应的编译或测试任务。
3. 不新增没有调用方的抽象层、缓存或兼容分支。
4. 不在适配脚本或 WebView 页面保存密码。
5. 提交前运行 `git diff --check` 和受影响模块的 Gradle 任务。
