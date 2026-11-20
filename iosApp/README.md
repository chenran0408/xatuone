# 一校通 · iPhone 预览版

适用于 iOS 16 及以上版本的 iPhone。打开「课程」登录学校教务系统，点「同步课表」选择学期。同步后的课表可以离线查看。切换登录身份时，各账号的课表与宿舍房间分别保存。

| 入口 | 可用操作 |
| --- | --- |
| 课程 | 同步学期课表，切换日视图与周视图，查看节次、教师、地点、课程序号和学分 |
| 教务 | 查询成绩与学分、考试、学业进度、培养方案和教学周，查看校历原图并识别文字 |
| 校园 | 选择宿舍并查询剩余电量，查看本机保存的余额趋势，打开签到、图书馆、校园网、充值和办事网页 |
| 我的 | 登录、重新认证或退出，清除本机网页登录状态 |

学校验证码在 App 内的学校网页完成。部分服务需要校园网或学校认可的接入方式。首次查询电费需要登录缴费平台。iPhone 使用系统原生界面，本版尚未移植 Android 桌面小组件和上课提醒。

## 安装

在本分支最近通过的 [iPhone 构建](https://github.com/chenran0408/xatuone/actions/workflows/ios-preview.yml?query=branch%3Aios%2Fcampus-preview) 下载 `XATU-One-iPhone-unsigned`，解压取得 IPA。下载 Actions 附件通常需要登录 GitHub。

在已配置好的 AltStore Classic 中打开 **My Apps → ＋**，选择 IPA，用自己的 Apple 账号签名安装。使用免费 Personal Team 时签名通常有效 7 天，可在到期前刷新。真机按系统提示开启开发者模式。[AltStore 使用说明](https://faq.altstore.io/altstore-classic/your-altstore) · [Apple 账号权限与签名期限](https://developer.apple.com/help/account/basics/about-your-developer-account)

## 构建与测试

在安装了 Xcode 和 XcodeGen 2.48.0 的 Mac 上，从仓库根目录运行：

```sh
bash iosApp/build-ipa.sh
cd iosApp
xcodebuild -showdestinations -scheme XatuOne
xcodebuild test -scheme XatuOne -destination 'platform=iOS Simulator,id=<可用 iPhone 模拟器的 UDID>'
```

真机 Release 包和 SHA256SUMS 位于 `iosApp/build/`。GitHub Actions 会检查 arm64、目标平台、包标识和适配脚本，并在模拟器测试通过后提供下载。
