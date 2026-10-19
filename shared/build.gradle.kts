plugins {
    alias(libs.plugins.bilibilias.multiplatform.library)
    alias(libs.plugins.bilibilias.multiplatform.koin)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildconfig)
}

kotlin {
    android {
        namespace = "com.imcys.bilibilias.shared"
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            // 具体配置在文件末尾的 Framework.configureSharedFramework() 中，
            // 那里对该实验性 API 做了显式 opt-in（transitiveExport 目前是
            // @ExperimentalKotlinGradlePluginApi 且已标记 Deprecated）。
            configureSharedFramework(project)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.device.compat)
            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.androidx.documentfile)
            implementation(libs.ffmpeg.kit.x6kb)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.compose.cloudy)
        }

        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            kotlin.srcDir("build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors")
            kotlin.srcDir("build/generated/compose/resourceGenerator/kotlin/commonResClass")
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.material.icons.extended.kmp)
                implementation(libs.kmp.androidx.lifecycle.runtimeCompose)
                implementation(libs.kmp.androidx.lifecycle.viewmodel)
                implementation(libs.kotlinx.datetime)
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.jetbrains.material3.adaptive)
                implementation(libs.jetbrains.material3.adaptive.layout)
                implementation(libs.jetbrains.material3.adaptive.navigation)
                implementation(libs.jetbrains.material3.adaptive.navigation3)
                implementation(libs.paging.compose)
                implementation(libs.koin.compose.kmp)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.confettikit)
                // 二维码本地生成（替代 pan.misakamoe.com 云端出图）
                implementation(libs.qrose)
                api(project(":core:data"))
                api(project(":core:common"))
                api(project(":core:ui"))
            }
        }
    }

}



buildConfig {
    className("ASBuildConfig")

    val enabledPlayAppMode: String by project
    val enabledAnalytics: String by project
    fun Project.optionalStringProperty(name: String): String = findProperty(name)?.toString() ?: ""
    val baiduStatId: String = project.optionalStringProperty("as.baidu.stat.id")
    val githubOrg: String = project.optionalStringProperty("as.github.org")
    val githubRepository: String = project.optionalStringProperty("as.github.repository")
    // 与 app 模块保持一致：固定 7 位，供安装包来源校验使用（详见 README「安装包来源校验」）。
    val gitCommitHash: String = providers.exec {
        commandLine("git", "rev-parse", "--short=7", "HEAD")
    }.standardOutput.asText.get().trim()

    buildConfigField("String", "BAIDU_STAT_ID", """"$baiduStatId"""".trimIndent())
    buildConfigField("String", "GIT_COMMIT_HASH", """"$gitCommitHash"""".trimIndent())
    buildConfigField("String","GITHUB_ORG",""""$githubOrg"""".trimIndent())
    buildConfigField("String","GITHUB_REPOSITORY",""""$githubRepository"""".trimIndent())
    buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", enabledPlayAppMode)
    buildConfigField("boolean", "ENABLED_ANALYTICS", enabledAnalytics)
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

/**
 * 配置 iOS 静态 framework。
 *
 * `transitiveExport` 目前带 `@ExperimentalKotlinGradlePluginApi` 且已标记 Deprecated，
 * 在脚本里直接使用会产生"实验性 API"警告。这里集中到带 opt-in 的函数中，
 * 既保留原有导出行为（让 framework 导出 :core:data 的传递依赖），又消除警告。
 */
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
@Suppress("DEPRECATION")
private fun org.jetbrains.kotlin.gradle.plugin.mpp.Framework.configureSharedFramework(
    project: org.gradle.api.Project,
) {
    baseName = "ASShared"
    isStatic = true
    export(project.project(":core:data"))
    transitiveExport = true
}
