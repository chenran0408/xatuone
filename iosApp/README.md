# 一校通 · iPhone 预览版

0.2.0 适用于 iOS 16 及以上版本的 iPhone。可以直接手动建课表，也可以在「课程 → 同步课表」登录教务、选择学期。课表在本机离线可用。

| 入口 | 可用操作 |
| --- | --- |
| 首页 | 查看今日课程、教学周、已查到的最近考试、宿舍余电和查询时间 |
| 课程 | 切换日／周视图和课表，编辑课程、批量调课、设置作息和调休，管理提醒、小组件、文件导入导出和系统日历同步 |
| 教务 | 按学期查成绩、学分和绩点，搜索课程，查看考试倒计时、学业进度、培养方案、教学周、校历原图和识别文字；查过的数据附更新时间 |
| 校园 | 选房、查余电、看每日记录和余额变化，保存设备位置、打开签到网页，以及图书馆、校园网套餐、充值和办事网页 |
| 我的 | 管理教务账号，更换导航外观、深浅色和启动页，通过「文件」或自己的 HTTPS WebDAV 备份与恢复课表 |

教务验证码和校园网套餐都在 App 内的学校网页办理，电费首次查询需要登录缴费平台。部分服务需要校园网。每个教务账号和免登录使用的本机课表分别保存。iOS 17 及以后可切换已保存的账号，每个账号使用独立的网页会话；iOS 16 切换账号需要重新登录。

课程提醒需允许发送通知，应用会在打开或修改课表时补充最近 48 次提醒。主屏幕课表小组件随 IPA 提供。标准自动更新需要主程序和扩展使用同一签名团队、并具有配置匹配的 App Group。没有共享组时，可在「课表设置 → 提醒与小组件」手动发送短期课表；重签时仍需保留小组件扩展。

Android 和 iPhone 之间可以用单张课表 JSON 交换课程、节次和组合作息，也可用 ICS 导出至日历。整机课表备份格式各平台独立。iPhone 备份保存当前使用者的课表及偏好，账号密码、登录凭据与校园服务查询记录不在备份文件中。

## 安装

在本分支最近通过的 [iPhone 构建](https://github.com/chenran0408/xatuone/actions/workflows/ios-preview.yml?query=branch%3Aios%2Fcampus-preview) 下载 `XATU-One-iPhone-unsigned`，解压取得 IPA。下载 Actions 附件通常需要登录 GitHub。

使用自己的 Apple 账号为 IPA 签名后安装。免费 Personal Team 的签名有效期为 7 天。[Apple 账号权限与签名期限](https://developer.apple.com/help/account/basics/about-your-developer-account)

## 构建与测试

在安装了 Xcode 和 XcodeGen 2.48.0 的 Mac 上，从仓库根目录运行：

```sh
bash iosApp/build-ipa.sh
cd iosApp
xcodebuild -showdestinations -scheme XatuOne
xcodebuild test -scheme XatuOne -destination 'platform=iOS Simulator,id=<可用 iPhone 模拟器的 UDID>'
```

未签名的真机 Release 包和 SHA256SUMS 位于 `iosApp/build/`。GitHub Actions 核对主程序、小组件、arm64、目标平台和适配脚本；在独立 iPhone 模拟器执行测试后提供下载。
