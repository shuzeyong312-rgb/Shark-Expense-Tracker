# Android 开发环境检查报告

检查日期：2026-11-04。范围仅为项目骨架，不包含 V1 业务功能。

## 电脑和工具

- Windows 11 专业版，版本 10.0.26300，64 位；AMD Ryzen 7 5800H，x86-64；物理内存约 15.36 GiB，检查时 C 盘剩余约 48.9 GiB。
- Android Studio 已安装：`C:\Program Files\Android\Android Studio`，构建号 262.10968.63.2622.16730706。已能打开项目窗口，说明并非停留在首次安装向导。
- 使用 Studio 自带 JBR 25.0.4：`C:\Program Files\Android\Android Studio\jbr`。命令行构建通过当前进程的 `JAVA_HOME` 指向它，没有更改全局环境变量，也没有安装独立 JDK 或全局 Gradle。
- SDK：`C:\Users\shuzeyong\AppData\Local\Android\Sdk`。`local.properties` 已指向此路径，`.gitignore` 排除了该文件。
- 已有 Platform 37（Android 17，非预览）、Build Tools 36.0.0 和 37.0.0、Platform Tools 37.0.1、Command-line Tools 23.0、Emulator 37.2.13、API 37 Google Play x86_64 镜像。`adb`、`sdkmanager`、`emulator` 均通过完整路径调用；无需额外安装构建组件。
- 项目使用 Gradle Wrapper 9.6.0（发行包 SHA-256 已校验），AGP 9.5.0，Kotlin Compose 插件 2.4.21，Compose BOM 2026.09.00，Navigation 3 1.2.0，Room 2.8.5，DataStore 1.3.0。`minSdk=26`、`compileSdk=37`、`targetSdk=37`。namespace 和 applicationId 暂为 `com.example.personalledger`，正式发布前待确认。

## 验证

| 项目 | 结果 |
| --- | --- |
| Wrapper | `gradlew --version` 成功；使用 Studio JBR 25.0.4 |
| Kotlin 编译、Debug 构建 | `:app:compileDebugKotlin :app:assembleDebug` 成功 |
| 单元测试 | `:app:testDebugUnitTest` 成功，入口类烟雾测试 1 项通过 |
| APK | `app/build/outputs/apk/debug/app-debug.apk`，12,820,928 字节；SHA-256 `95ec98788781190853718e94196c3715a78e23694c270b938d8f462fac6601e9` |
| Manifest | 检查合并结果：min 26、target 37、启动 Activity 存在；没有 INTERNET 权限；allowBackup=false，两个备份排除规则已引用 |
| 模拟器 | 现有 `Medium_Phone_API_37.0` 使用 WHPX；`-accel-check` 成功，模拟器启动完成；APK 通过 adb 安装，MainActivity 为 top resumed activity |
| Android Studio 导入 | 项目文件夹已打开，但 Gradle 同步尚未验证成功。首次同步因 IDE 中选择的 `JAVA_HOME` 宏未定义失败；应在错误面板选择 **Use Embedded JDK**，或在 Gradle JDK 设置选择 `jbr-25`，然后重新同步。不要选择下载 JDK。 |
| 冻结资料 | Spec、图标映射、HTML 原型的 SHA-256 均与原 `SHA256SUMS.txt` 一致。README 按本任务更新，其原始校验值不再匹配。 |

CPU 固件虚拟化已启用，Windows Hypervisor Platform 可用。模拟器提示可用内存 15724 MiB，略低于其建议的 16384 MiB，但本次仍成功启动；内存紧张时可关闭其他应用。当前未连接实体手机。实体机方案：启用开发者选项和 USB 调试，用数据线连接并在手机上确认调试授权；用 SDK `platform-tools\adb devices` 检查状态，然后在 Studio 运行或用 `adb install -r` 安装。

## 当前执行环境的临时处理和未完成事项

在 Codex 当前命令执行环境中，JBR 的 Java NIO 初始化报 `Unable to establish loopback connection`（UnixDomainSockets `Invalid argument`），导致 Gradle 和 SDK Manager 初次失败。仅在对应 PowerShell 进程设置 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\nonexistent-ledger-socket` 后，SDK Manager、构建及测试均成功；该目录不存在，此设置没有写入项目或系统环境。若普通终端或 IDE 未出现该错误，不需要设置它。

Android Studio 的 Gradle 同步仍需在界面确认成功；如仍失败，查看 IDE 同步错误并按实际提示处理。未测试实体手机、不同 API 版本、正式签名和发布。Debug 构建提示两个依赖的原生库无法 strip，已原样打包，不影响本次 Debug 构建和模拟器启动。

Room、DataStore、Navigation 3 和协程仅配置依赖，未创建数据库或业务逻辑；Compose Canvas 与 SAF 为平台能力，待正式开发按需使用。当前目录不是 Git 仓库，未创建远程仓库或推送。
