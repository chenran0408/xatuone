# 独立仓库说明

一校通从拾光课程表仓库独立维护，原因是当前工程已经包含 XATU 专属登录、校园服务、学术数据和 Android 交互，发布节奏与通用课程表项目不同。

## 迁移范围

- 保留现有课程表、提醒、导入导出和小组件能力。
- 保留当前 Android WebView 登录会话和 XATU 服务实现。
- 保留 `shared`、`androidApp`、`desktopApp` 和 `iosApp` 的现有构建结构，后续按实际使用情况再决定是否收敛平台。
- 通用课程适配脚本继续遵循拾光适配协议，适配仓库与本项目分开维护。

## 远程仓库设置

创建 GitHub 仓库后，在本目录执行：

```text
git remote add origin https://github.com/chenran0408/xatuone.git
git push -u origin main
```

原拾光仓库保持原有远程和分支，不在本仓库配置 `upstream`，避免把一校通提交误推回通用课程表项目。
