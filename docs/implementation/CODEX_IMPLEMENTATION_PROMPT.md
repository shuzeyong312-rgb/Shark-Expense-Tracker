# Codex 任务：一次性实现个人记账 APP Android V1

当前目录是本次 Android 项目的根目录。请先读取以下三份资料，并以它们为唯一实施依据：

- `docs/specs/v1-spec.md`：已冻结的 V1.6 业务与技术 Spec。
- `docs/prototypes/final-prototype.html`：用户已验收的唯一 UI 视觉/结构基线。
- `docs/specs/icon-mapping.md`：当前原型图标映射与本地资源规则。

## 执行要求

1. 检查当前目录和 Android/JDK/SDK/Gradle 环境；若没有 Android 项目，初始化单模块 Android Studio 项目。保留现有有效项目配置，不做无关重构。
2. 从 `main` 创建分支 `codex/android-v1-implementation`；若尚无 Git 仓库，初始化仓库并建立 `main` 后再创建该分支。禁止使用 Git Worktree；不擅自创建远程仓库、推送或合并 main。
3. 严格实现 Spec 全部 P0，不缩减范围、不增加 Spec 外功能、不重新设计 UI。必须使用原生 Jetpack Compose，不得用 WebView 承载 HTML 原型。
4. 按 Spec 使用 Kotlin、Jetpack Compose、Room/SQLite、DataStore、Coroutines/Flow、Navigation 3 稳定版（Spec 锁定 1.2.0）、Compose Canvas 图表、Storage Access Framework；保持单模块和简单 AppContainer，不引入 Hilt、后端或不必要依赖。`minSdk=26`，compile/target SDK 使用实施时最新稳定版本。
5. 按图标映射表从已验收 HTML 原型提取 SVG 并转换为本地资源；不得用 Emoji、默认 Material 图标或另一套第三方图标替换。无需下载外部图标库。
6. 实现 Room 数据模型、分类初始化、账单 CRUD、一级分类管理、二级备注标签、周/月/年统计、分类/备注详情、CSV 导出、JSON 完整备份及完整替换恢复。恢复前警告并二次确认，完整校验后通过单个 Room 事务替换。
7. 严格遵守本地优先与隐私约束：核心功能离线可用；Manifest 不声明 `INTERNET`；关闭 Android Auto Backup 和设备迁移备份；不接入登录、云同步、广告、分析 SDK、崩溃上报 SDK 或模型 API。
8. 按原型完成全部页面和交互，包含明细页固定头部/上拉切换上月、两阶段记账、分类设置/自定义图标、图表与分类详情同高固定头部、账伴静态聊天页。原型模拟账单不得写入首次安装数据。
9. 一次性持续执行到功能实现、测试、修复和自检完成。普通技术问题自行解决，不要反复暂停向用户索取已写入 Spec 的信息；只有确实存在无法从 Spec/原型判断且会阻断正确实现的业务冲突才汇报。
10. 至少执行 Kotlin 编译、Android Lint、单元测试、Room 测试、ViewModel 测试、关键 Compose UI 测试、Debug APK 构建和 Release APK 构建；覆盖 10,000 笔账单查询/统计。若缺少真机或模拟器，完成可运行的自动化检查并明确记录未能执行的真机项，不得伪称通过。

## 交付物

- 完整 Android Studio 项目源码。
- 可安装 Debug APK；Release APK（签名条件不足时提供可构建的未签名 APK 并说明）。
- 自动化测试、Lint 和构建结果。
- `README.md`：环境、构建、安装、CSV 导出及 JSON 备份/恢复说明。
- `docs/implementation/icon-assets-report.md`：图标资源、映射、来源和转换验证。
- `docs/implementation/implementation-report.md`：P0 完成情况、测试结果、已知限制和真机验收情况。
- Git 分支、最终提交 SHA、主要变更文件清单。

完成标准：Spec 全部 P0 已实现；关键测试通过；APK 构建成功；无已知 P0/P1 缺陷；UI 按冻结原型完成对照验收。不要只交计划、脚手架或阶段性半成品。
