# 工程审计记录

审计基线：`main`，工作区包含一校通 Android 改造的未提交修改。审计没有重置或覆盖这些修改。

## 技术栈

- Kotlin Multiplatform、Compose Multiplatform 1.11.1、Navigation 3。
- Android 使用 Koin、Room 3、DataStore、Ktor/OkHttp、WebView 和 ML Kit OCR。
- Android Release 已启用 R8 和资源压缩。

## 发现的问题

### 高风险

- 校园快照原先只有单份 DataStore JSON，宿舍、电费历史、签到坐标和校历可能随账号切换泄露给下一账号。
- WebView 的原生桥接对象在页面创建时全局挂载，写入型桥接缺少来源边界。
- 教务缓存包含成绩、考试和培养方案，保存为明文 JSON。
- Release 默认使用 Debug 签名，容易误把本地验证包当正式包。
- 仓库没有业务单元测试，解析、缓存和账号切换缺乏回归保障。

### 中风险

- XATU 页面在多个 `LaunchedEffect` 中直接发起请求，状态模型分散。
- XATU service 默认创建 OkHttpClient，部分文件通过 Koin 全局容器取 Context，测试替换困难。
- 教务地址使用 HTTP，网络安全配置全局允许明文流量，需要确认学校服务是否支持 HTTPS 后再收紧范围。
- CI 只由 `workflow_dispatch` 触发，没有常规 PR/push 质量门禁。
- README 尚未描述一校通模块、WebView 会话和新增服务流程。

## 处理顺序

1. 账号隔离、桥接来源校验和退出登录清理。
2. 建立解析与数据计算的 host tests。
3. 修正发布签名和 CI 质量门禁。
4. 再逐步统一 XATU 页面状态模型和网络依赖注入。

## 保留的设计

当前不拆分新的 Gradle module。KMP source set、Koin、Room、DataStore 和 Navigation 3 已能支撑当前规模，先通过目录边界和接口约定治理复杂度。

## 第一阶段已实施

- 校园快照和签到位置在账号切换、退出登录时清理，学术缓存按账号清理。
- 自定义签到位置桥接在原生接口层校验 XATU 签到页面来源。
- 增加 Android host tests，覆盖坐标转换和学期解析。
- 本地 Release 不再默认使用 Debug 签名；CI 增加 PR 和 push 触发、编译与 lint 门禁。
- 增加 `AGENTS.md` 和架构说明，固定新增校园服务的数据流和验证流程。

## 尚未处理

- 教务系统当前使用 HTTP，是否能全面切换 HTTPS 需要先确认学校服务端能力。
- 学术缓存仍为 App 私有目录中的明文 JSON，后续再评估 Keystore 加密和迁移策略。
- 通用课程导入 Bridge 仍需结合适配脚本来源建立兼容的来源校验方案。
- XATU 页面请求状态仍有多个 `LaunchedEffect`，后续可在不改变接口的前提下收敛到 ViewModel 状态模型。
