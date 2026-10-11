# 构建矩阵与开关组合

本文档说明 BILIBILIAS 当前 flavor、build type 与 `gradle.properties` 开关的实际组合规则。目标是避免只看单个配置文件就误判最终产物行为。

> **Google Play 渠道：历史遗留，不再使用。**
> 本项目已停止通过 Google Play 分发，且 Firebase / Google Play Core / 百度统计等闭源依赖
> 已从仓库整体移除。`beta` flavor 与 `enabledPlayAppMode` 开关仅作为历史遗留保留，
> **不会也不可能再用于发布**。详见下文「依赖启停规则」。

## 总览

当前构建结果主要由三层共同决定：

1. `gradle.properties`
2. `app/build.gradle.kts` 中的 `productFlavors`
3. `app/build.gradle.kts` 中的 `buildTypes`

其中 `enabledAnalytics` 已不再有任何实际作用（相关依赖与实现均已移除），
`enabledPlayAppMode` 属于历史遗留开关，两者都只影响少量遗留代码分支。

## Flavor 定位

### `alpha`

- **日常开发、测试打包、快速验证的首选，也是当前的唯一实际使用渠道**
- 带 `applicationIdSuffix` 和 `versionNameSuffix`
- 本地默认使用 debug signing
- 在 CI 中可读取 `RUNNER_TEMP/mxjs-debug.jks`
- `ENABLED_PLAY_APP_MODE` 被 flavor 固定写成 `false`

### `beta`（历史遗留，不再使用）

- 原用途：Google Play 提交
- 带 `applicationIdSuffix` 和 `versionNameSuffix`
- 使用 `BILIBILIASSigningConfig`
- `ENABLED_PLAY_APP_MODE` 读取 `gradle.properties` 中的 `enabledPlayAppMode`
- **本项目已停止 Google Play 分发，该 flavor 不再用于任何发布流程**

### `official`（历史遗留）

- 原用途：最终正式发行渠道，使用正式 `applicationId`
- 使用 `BILIBILIASSigningConfig`
- **项目已停止公开发布 APK**，该 flavor 保留仅供自行编译使用

## Build Type 行为

### `debug`

- 用于本地调试
- 写入 `ENABLED_PLAY_APP_MODE`
- 写入 `ENABLED_ANALYTICS`
- ABI split 关闭

### `release`

- 开启 R8 压缩
- 开启资源裁剪
- 写入 `ENABLED_PLAY_APP_MODE`
- 写入 `ENABLED_ANALYTICS`
- ABI split 开启
- 生成 universal APK

## 开关影响范围

### `enabledPlayAppMode`（历史遗留）

**结论：保持 `false`，并视为永久 `false`。**

当前实际影响已非常有限：

- `BuildConfig.ENABLED_PLAY_APP_MODE` 的取值（`alpha` 下恒为 `false`）
- 少量 UI 是否进入「Play 渠道」分支（例如设置页仓库/反馈入口的显隐判断）

已经**不再**影响：

- Google Play 更新/评价依赖：`com.google.android.play:*` 条目已从版本目录移除，
  不存在 `implementation` ↔ `compileOnly` 的切换
- 应用内更新能力：`GooglePlayAppUpdateManage` 已是空实现，
  所有 `checkApp*Update()` 固定返回 `false`，设为 `true` 也不会产生真实商店更新
- 百度统计可用性：百度统计已整体移除

因此把它设为 `true` 不会带来任何真实能力，只会打开少量无实际作用的历史分支。

### `enabledAnalytics`

**已无实际作用**，仅保留 `BuildConfig.ENABLED_ANALYTICS` 字段的生成
（部分遗留代码仍会读取它）。Firebase 与百度统计均已移除：

- 不存在「Crashlytics / Analytics / Config / Messaging / Perf / In-App Messaging
  是 `implementation` 还是 `compileOnly`」的切换——版本目录条目已删除
- 不存在百度统计 jar 的引入逻辑
- 埋点实现是同名 No-Op 替身，不会采集或上传任何数据

隐私授权逻辑仍然保留：未同意隐私政策时不会触发任何相关调用。

### `as.github.org` / `as.github.repository`

影响：

- 应用内版本检查所访问的 GitHub 仓库信息，当前为 `Zhenghao-Wen` / `Analytics`

### `as.baidu.stat.id`

影响：

- Manifest placeholder
- `BuildConfig.BAIDU_STAT_ID`

注意：百度统计已移除，该值当前不再被任何实现使用，仅保留字段以免影响历史构建配置。

## 常见组合

### 日常开发（推荐）

```bash
./gradlew :app:assembleAlphaDebug
```

特点：

- Play 模式固定关闭
- 适合大多数页面、下载、解析、导航和设置变更

### 本地验证 release 行为（推荐）

```bash
./gradlew :app:assembleAlphaRelease
```

特点：

- 仍是 `alpha` 渠道
- 可以提前验证 R8、资源裁剪、ABI split
- 这是当前产出发行包的实际用法

### 历史遗留渠道（不建议）

```bash
./gradlew :app:assembleBetaRelease
./gradlew :app:assembleOfficialRelease
```

这两个任务对应已停止使用的分发渠道。它们仍会受签名与本地/CI 凭据影响；
**项目已停止公开分发，请勿据此假定存在可用的发布流程**。

## 依赖启停规则

**Firebase / 百度统计 / Google Play 的启停规则均已失效。** 这些依赖已从本仓库整体移除：

- `firebaseDependencies()` / `googlePlayDependencies()` / `baiduStatDependencies()`
  三个函数及对应插件（`gms.google.services`、`firebase.crashlytics`、`firebase.perf`、
  `bilibilias.baidu.jar`）都已删除；
- 版本目录中的 Firebase、`com.google.android.play:*` 条目已清理；
- 不存在 `implementation` ↔ `compileOnly` 的切换了，构建脚本中也没有任何
  指向它们的引用。

源码中的 Firebase / Google Play / 百度统计调用由**本仓库内的同名 No-Op 替身**接管
（`app/src/main/java/com/google/firebase/`、`app/src/main/java/com/baidu/mobstat/`，
以及 `common/update/GooglePlayAppUpdateManage.kt`、
`common/utils/firebase/FirebaseNetworkPerformanceTracer.kt`）。

R8 需要 `-dontwarn` 规则来忽略已移除依赖的残留引用，见 `app/proguard-rules.pro`。

## 签名与本地限制

- `official` / `beta` 使用 `BILIBILIASSigningConfig`（均为历史遗留渠道）
- 仓库未内置 keystore 与密码
- `alpha` 本地默认回落到 debug signing
- `alpha` CI 可通过 `RUNNER_TEMP` 提供单独签名文件

因此如果某个 release 构建因签名失败，不要在文档或回复里假定“构建成功”，应直接说明缺少本地签名条件。

## 文档维护约定

以下改动应同步更新本文档：

- flavor / build type 调整
- `enabledPlayAppMode` / `enabledAnalytics` 行为变化
- Google Play / Firebase / 百度统计依赖启停逻辑变化
- ABI split、签名、发布流程变化
