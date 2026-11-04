# 个人记账 APP V1 · Android 开发交接包


本资料包是交给 Codex 的正式开发输入。**UI 原型已于 2026-11-04 通过用户验收，V1.6 Spec 与原型现已冻结。** 本包不包含 Android 业务代码；Codex 应在本目录中初始化并实现 Android Studio 项目。

## 1. 交接基线

| 项目 | 冻结决定 |
|---|---|
| 产品范围 | V1 Core |
| 平台 | 仅 Android 手机原生 APP |
| 电脑辅助 | 通过 CSV/JSON 文件查看、导出和备份；无实时同步、无 PC 客户端 |
| UI | `docs/prototypes/final-prototype.html`，用户已验收 |
| Spec | `docs/specs/v1-spec.md`，V1.6 FROZEN |
| 技术栈 | Kotlin + Jetpack Compose + Room/SQLite + DataStore + Coroutines/Flow + Navigation 3 + Compose Canvas |
| 数据策略 | 本地优先、离线可用、不登录、不联网、不接后端 |
| 备份恢复 | JSON 完整备份；恢复采用完整替换，恢复前警告并二次确认 |
| 产品命名 | 正式名称未冻结；开发期称“个人记账 APP”，不使用鲨鱼记账商标、Logo 或未授权素材 |

## 2. 文件结构

```text
shark-ledger-v1-development-handoff/
├── README.md
├── SHA256SUMS.txt
└── docs/
    ├── specs/
    │   ├── v1-spec.md
    │   └── icon-mapping.md
    ├── prototypes/
    │   └── final-prototype.html
    └── implementation/
        └── CODEX_IMPLEMENTATION_PROMPT.md
```

## 3. 使用方式

### 当前 Android 骨架

本次仅初始化环境和最小 Compose 入口，不包含记账功能。使用 Android Studio 打开本目录，加载 Gradle 项目；在 **设置 → 构建、执行、部署 → 构建工具 → Gradle** 中选择 Android Studio 自带 JBR，然后同步。SDK Manager 需要 Android SDK Platform 37、Build Tools 及 Platform Tools。`local.properties` 是本机 SDK 路径，不提交；换电脑后由 Android Studio 重新生成。

在 PowerShell 的当前会话中使用 Studio 自带 JBR（不修改系统环境变量）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。Android Studio 的 Device Manager 可启动已有的 `Medium_Phone_API_37.0`，选择 `app` 运行；也可以连接已开启开发者选项和 USB 调试的手机，确认手机上的授权提示后运行，或使用 SDK 中的 `platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk` 安装。Debug APK 只用于开发测试。

如在 Codex 当前命令执行环境遇到 `Unable to establish loopback connection`，可在该 PowerShell 会话临时设置 `$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:\nonexistent-ledger-socket'` 后重试；不要创建该目录。详细结果见 `docs/implementation/environment-setup-report.md`。正式发布前需确认 namespace 和 applicationId，当前均为 `com.example.personalledger`。

以下为原交接包的后续正式开发说明，本次未执行：

1. 下载并解压整个 ZIP，保留目录结构。
2. 使用 Codex 打开解压后的 `shark-ledger-v1-development-handoff` 根目录。
3. 将 `docs/implementation/CODEX_IMPLEMENTATION_PROMPT.md` 作为一次性完整开发任务交给 Codex。
4. Codex 必须先读取冻结 Spec、最终原型和图标映射，再直接完成实现、测试、修复和交付。

## 4. 重要边界

- `final-prototype.html` 是视觉和交互结构基线，不是正式 APP 代码；正式 UI 必须使用 Jetpack Compose。
- HTML 原型里的模拟账单仅供预览，首次安装必须为空账单，只初始化系统分类。
- 图标已内嵌在 HTML 原型中，优先提取并转为本地 Android 资源，不需要你手动下载图标。
- 不允许擅自增加 AI、预算、账户余额、多账户、云同步、登录、广告、会员、CSV 导入等功能。
- Spec 与原型已冻结；任何需求或 UI 变更必须先更新 Spec 并获得用户确认。

## 5. 基线校验

- UI 原型 SHA-256：见 `SHA256SUMS.txt`。
- 冻结 Spec SHA-256：见 `SHA256SUMS.txt`。
- 交接包其他关键文件的 SHA-256 同样记录在 `SHA256SUMS.txt`。
- `SHA256SUMS.txt` 保留原始交接包校验值；本 README 已按环境初始化任务更新，因此其原始校验值不再匹配。
