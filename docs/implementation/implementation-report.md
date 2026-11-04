# V1 实施报告

## 基线与范围

- Spec：`docs/specs/v1-spec.md`，V1.6 FROZEN；原型：`docs/prototypes/final-prototype.html`，SHA-256 `76e0836cb09817b2117ff5f5e03a5848958b2bfa7465edbdb5d8d96ed990994f`。
- Android 原生 Jetpack Compose，单模块；Room 数据库版本 1，模式文件在 `app/schemas/`。尚无需要从更早数据库版本升级的迁移路径；后续模式升级须提供非破坏性迁移和迁移测试。
- 应用不声明 INTERNET；Debug 与 Release 的合并 Manifest 已核对 `allowBackup=false`，包含排除云备份和设备迁移的规则。

## P0 实现情况

| 范围 | 实现 |
| --- | --- |
| 明细 | 当前月和月份选择、收支与按日汇总、固定头部、账单编辑/长按操作；空月也可上拉查看上月。 |
| 记账和分类 | 支出/收入分类选择与底部金额面板、整数分金额、备注、日期、编辑及删除确认；分类显示/隐藏、拖动排序、系统分类和自定义分类管理。 |
| 备注与统计 | 备注标签按分类隔离、自动复用，快捷标签按历史次数和日期排序；周/月/年趋势、分类排行、备注排行、环形图与详情。大金额汇总使用 BigInteger 避免 Long 汇总溢出。 |
| 我的与账伴 | 记账天数、总笔数、分类管理及数据管理；账伴为静态界面，输入、附件与发送按钮禁用。 |
| 数据管理 | SAF CSV 导出（文本公式前缀防护）、JSON 备份、先验证再双重确认的完整替换恢复；恢复写入采用单个 Room 事务。 |

## 自动化和构建

最后一轮执行：`:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug :app:assembleRelease`，Gradle 报告 BUILD SUCCESSFUL。

- 本地单元测试 7 项通过；模拟器设备测试 22 项通过、0 失败。设备为 `Medium_Phone_API_37.0`。涵盖 Room、ViewModel、Compose 记账/分类/图表、跨年上拉与月份确认、隐私 Manifest、备份校验与事务失败回滚，以及 10,000 笔账单的读取和统计。
- Debug Lint：0 错误、11 警告；警告包括依赖版本建议、版本目录建议、滚轮重组提示及未设置正式应用图标。未仅因版本建议升级 Gradle 或既有依赖。
- 模拟器实际通过系统文件选择器导出空账本 JSON 和 CSV，检查 JSON 数据结构及 CSV BOM，经过两次确认后恢复成功；非空数据往返与故障回滚由 Room 测试覆盖。
- 模拟器实际检查一笔账单的新增、月统计、分类/备注详情及删除；也检查 320dp 宽度、记账面板数字键盘可见性，以及输入法弹出时金额和备注区域。
- Debug APK 已安装于模拟器并由 `apksigner verify` 验证签名；SHA-256 `b8d59743ce01bf8c5645601f8e3991fce5ce468642dc9c2f5986c39377a691de`。Release APK 可构建但未签名，SHA-256 `8d0cbdcc8c1969e47296b6752bd805ffc763564f5d0304f0044e7adfa03bda5a`，不可直接作为正式发布包。
- 自动化结果位于 `app/build/reports/tests/testDebugUnitTest/`、`app/build/reports/androidTests/connected/debug/` 和 `app/build/reports/lint-results-debug.html`。Gradle 测试覆盖关键路径，并非穷尽所有设备和输入。

## 尚需验收与限制

- 当前未连接实体手机，尚未进行真机安装、不同 Android 版本与厂商适配、手势、输入法、SAF 文件提供方及完整记账/恢复流程验收。
- 已在模拟器对照明细、记账、图表、分类详情、备注详情、分类管理、我的及账伴页面；尚未逐项完成所有状态、字号、不同屏幕和真机的像素级视觉对照，不应将此记为用户对原生 UI 的验收。
- 图标映射与转换细节见 `icon-assets-report.md`；台球图标的数字 8 是转换路径近似，尚需视觉确认。
- Release 需正式签名；产品名、`applicationId` 和正式应用图标仍需发布前确定。本次未配置发布签名、推送或合并。
- 本次没有已知仍失败的自动化测试；上述真机和完整视觉验收仍未执行或未完成，因此不能将其标记为已通过。
