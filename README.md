# 个人记账 APP V1

Android 原生、离线记账项目。产品和界面基线分别为 `docs/specs/v1-spec.md`（V1.6）与 `docs/prototypes/final-prototype.html`。首次启动只建立系统分类，不生成示例账单。

`SHA256SUMS.txt` 保留原交接包校验值；README 在开发期间已更新，其历史校验值不再适用。

## 环境与构建

- Android Studio、Android SDK Platform 37 和 Build Tools 37；本机验证使用 Android Studio 自带 JBR 25。
- 单模块，Gradle Wrapper 9.6.0，AGP 9.5.0，Kotlin 2.4.21。`local.properties` 配置本机 SDK 路径，不进入 Git。
- Windows PowerShell 示例：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug :app:assembleRelease
```

`connectedDebugAndroidTest` 需要已启动的模拟器或授权设备。若当前终端出现 `Unable to establish loopback connection`，可仅在当前进程设置 `$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\nonexistent-ledger-socket'` 后重试；无需创建该目录。

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`，可用 `adb install -r` 安装。Release 位于 `app/build/outputs/apk/release/app-release-unsigned.apk`；未配置正式签名，发布或安装前须由持有签名密钥的人完成签名。正式产品名、applicationId 和签名需在发布前确认；当前 applicationId 为 `com.example.personalledger`。

## 使用和数据

- 通过底部记账入口选择支出或收入分类，选择分类后输入金额、备注与日期；账单可点击编辑，长按打开操作，删除前确认。
- “我的 → 导出账单数据”使用系统文件选择器导出 UTF-8 BOM CSV，供 Excel/WPS 查看；CSV 不支持导入。为避免表格软件执行公式，可能被识别为公式的分类名和备注会在 CSV 中添加单引号；JSON 备份保留原文。
- “我的 → 数据备份”使用系统文件选择器导出完整 JSON。JSON 是未加密文件，请自行妥善保管。
- “我的 → 从备份恢复”选择 JSON，程序校验数据并显示数量；两次确认后会完整替换本机账务数据。取消或校验失败不会执行恢复。
- 核心数据使用本机 Room 数据库，轻量页面偏好使用 DataStore；无登录和云同步。应用不声明 INTERNET，已配置排除 Android 系统云备份与设备迁移。

测试、图标与已知验收范围见 `docs/implementation/implementation-report.md` 和 `docs/implementation/icon-assets-report.md`。真机验收未由自动构建替代。
