# 工程审计记录

审计基线：`main`，工作区包含一校通 Android 改造的未提交修改。审计没有重置或覆盖这些修改。

## 技术栈

- Kotlin Multiplatform、Compose Multiplatform 1.11.1、Navigation 3。
- Android 使用 Koin、Room 3、DataStore、Ktor/OkHttp、WebView 和 ML Kit OCR。
- Android Release 已启用 R8 和资源压缩。

## 初始发现与当前状态

### 高风险

- 校园快照原先只有单份 DataStore JSON，宿舍、电费历史、签到坐标和校历可能随账号切换泄露给下一账号；目前账号切换和退出登录会清理对应数据。
- WebView 的原生桥接对象在页面创建时全局挂载；课程写入动作现在要求先由宿主设置当前导入课表并进入导入会话，但尚未建立通用的 Web 来源白名单。
- 教务缓存原先保存为明文 JSON；目前使用 Android Keystore 加密，读取旧版明文缓存后会在下一次写入时迁移。
- Release 原先默认使用 Debug 签名；目前本地 Release 不绑定 Debug 签名，正式签名由 CI 注入。
- 仓库原先没有业务单元测试；目前 Android host tests 覆盖教务解析和坐标转换，CI 会运行这些测试。

### 中风险

- 教务系统仍提供 HTTP 接口，业务无法在当前服务端能力下完全切换 HTTPS；明文流量已限制到 `jwgl2018.xatu.edu.cn` 域名，其他域名默认拒绝。

## 已完成阶段

1. 账号隔离、导入会话门控和退出登录清理。
2. 建立解析与数据计算的 host tests。
3. 修正发布签名和 CI 质量门禁。
4. 统一 XATU 页面状态模型和网络依赖注入。

## 保留的设计

当前不拆分新的 Gradle module。KMP source set、Koin、Room、DataStore 和 Navigation 3 已能支撑当前规模，先通过目录边界和接口约定治理复杂度。

## 第一阶段已实施

- 校园快照和签到位置在账号切换、退出登录时清理，学术缓存按账号清理。
- 自定义签到位置桥接在原生接口层校验 XATU 签到页面来源；通用课程导入桥接通过宿主导入会话门控写入动作。
- 增加 Android host tests，覆盖坐标转换和学期解析。
- 本地 Release 不再默认使用 Debug 签名；CI 增加 PR 和 push 触发、编译与 lint 门禁。
- 增加 `AGENTS.md` 和架构说明，固定新增校园服务的数据流和验证流程。

## 已处理

- XATU 学术页面的成绩、考试和学业方案请求已收敛到 `XatuAcademicViewModel`，页面销毁、账号变更和学期切换会取消旧请求。
- WebView 和学术请求的会话、缓存与账号边界已在 Android 登录流程中统一处理。
- 教务系统当前仍使用 HTTP；网络安全配置仅为该教务域名保留明文例外，其他网络请求要求 HTTPS。
- 学术缓存已使用 Android Keystore 加密，读取旧版明文缓存时会在下一次写入时迁移为密文。
- 通用课程导入 Bridge 当前只保证“用户主动启动导入后才允许写入”，不声明 Web 来源白名单能力；如果适配器生命周期改变，需要同步调整导入会话门控。
- XATU 的 CookieManager、OkHttpClient 和校历服务已改为由 Koin 统一提供，页面不再自行创建网络服务实例。
