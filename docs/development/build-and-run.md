# 构建与运行

## 环境要求

- Android Studio 或命令行 Android SDK。
- JDK 17 用于 Gradle/build-logic；Android/Kotlin 编译目标为 Java 11。
- Android Gradle Plugin 9.2.0、Kotlin 2.3.0、Gradle wrapper 以仓库为准。
- Android SDK compileSdk 37。

优先使用仓库内 wrapper：

```bash
./gradlew --version
```

## 本地配置

根目录 `gradle.properties` 包含默认构建开关：

```properties
org.gradle.parallel=true
org.gradle.configureondemand=false
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
enabledPlayAppMode=false
enabledAnalytics=true
as.baidu.stat.id=0f9d51ff82
as.github.org=Zhenghao-Wen
as.github.repository=Analytics
```

这些值会影响 app 的 BuildConfig、依赖启用方式和运行时行为：

- `org.gradle.*` 开关用于启用并行执行、构建缓存和配置缓存；`configuration-cache.problems=warn` 用于兼容部分第三方 Gradle 插件任务。
- `enabledPlayAppMode`：**请保持 `false`**。Play Core 依赖已从版本目录移除、
  `GooglePlayAppUpdateManage` 已是空实现，设为 `true` 不会带来真实的商店更新能力，
  只会打开部分 UI 的 Play 分支（详见 [构建矩阵与开关组合](./build-matrix.md)）。
- `enabledAnalytics` 目前无实际作用：Firebase / 百度统计已整体移除，埋点为空壳实现
  （见下文「Firebase / Google Services / 百度统计（均已移除）」）。
- `as.github.*` 用于应用内版本检查。
- FFmpeg 运行能力来自 `app` 中的第三方依赖 `libs.ffmpeg.kit.x6kb`。

但要注意：这些属性不是所有 flavor 都原样继承。比如 `alpha` flavor 会把 `ENABLED_PLAY_APP_MODE` 固定写成 `false`，因此仅修改 `gradle.properties` 并不会让 `alpha` 变体进入 Play 模式。详细组合规则见 [构建矩阵与开关组合](./build-matrix.md)。

`local.properties` 是本机 Android SDK 配置，不应提交。

## 构建变体

Product flavor：

- `official`：正式渠道，`app_channel=Official`。
- `alpha`：带 applicationId/versionName suffix，本地默认使用 debug signing，CI 可读取 `RUNNER_TEMP` 下的 `mxjs-debug.jks`。
- `beta`：**历史遗留，不再使用**。原为 Google Play 提交用途，`app_channel=Beta`。
  本项目已停止 Google Play 分发。

日常开发、测试打包和本地验证优先使用 `alpha`。`official` 是最终正式发行渠道，不作为默认测试变体。

Build type：

- `debug`：用于本地调试。
- `release`：开启 R8 minify 和 resource shrink，并按 ABI split 输出。

ABI：

- 默认支持 `armeabi-v7a`、`arm64-v8a`、`x86_64`。
- debug 构建禁用 ABI split。
- release 构建启用 ABI split，并生成 universal APK。

当前 `core/ffmpeg` 目录没有参与构建；实际打包使用的是 `app` 中直接声明的 `ffmpeg-kit` 依赖。

## 常用命令

构建 debug APK：

```bash
./gradlew :app:assembleAlphaDebug
```

构建测试 release APK：

```bash
./gradlew :app:assembleAlphaRelease
```

最终正式发行前再构建 official release：

```bash
./gradlew :app:assembleOfficialRelease
```

运行单元测试：

```bash
./gradlew test
```

运行 Android lint：

```bash
./gradlew lint
```

只编译 Kotlin/Java 以快速暴露编译错误：

```bash
./gradlew :app:compileAlphaDebugKotlin
```

构建 iOS 可导入 `XCFramework`：

```bash
./gradlew :shared:assembleASSharedReleaseXCFramework
```

详细说明见 [iOS 导入产物](./ios-artifacts.md)。

## 签名

`official` 和 `beta` flavor 使用名为 `BILIBILIASSigningConfig` 的 signing config，但仓库内没有硬编码 keystore。release 签名需要由本地或 CI 注入。

不要把 keystore、密码、个人证书、Play 上传凭据提交到仓库。

## Firebase / Google Services / Google Play / 百度统计（均已移除）

本仓库已剥离全部闭源可选依赖，**不再应用**以下插件，`google-services.json` 也已删除：

- `com.google.gms.google-services`
- `com.google.firebase.crashlytics`
- `com.google.firebase.firebase-perf`
- `bilibilias.baidu.jar`

相应地：

- 源码中的 Firebase / Google Play / 百度统计调用已由**本仓库内的同名 No-Op 替身**接管
  （`app/src/main/java/com/google/firebase/`、`app/src/main/java/com/baidu/mobstat/`，
  以及 `common/update/GooglePlayAppUpdateManage.kt`、
  `common/utils/firebase/FirebaseNetworkPerformanceTracer.kt`）。
- 版本目录中 Firebase、`com.google.android.play:*`、百度统计相关条目已清理，
  构建脚本中已无 `firebaseDependencies()` / `googlePlayDependencies()` /
  `baiduStatDependencies()` 等函数。
- `enabledAnalytics` 目前不再有任何实际作用：埋点实现是空壳，不会采集或上传数据。
  隐私授权逻辑仍然保留（未同意时不触发任何相关调用）。
- 百度统计 jar 不再由任何插件下载；`app/libs/` 为空。
- R8 需要 `-dontwarn` 规则来忽略这些已移除依赖的残留引用，见 `app/proguard-rules.pro`。


