import com.android.build.api.variant.BuildConfigField
import com.imcys.bilibilias.buildlogic.BILIBILIASBuildType

plugins {
    alias(libs.plugins.bilibilias.android.application)
    alias(libs.plugins.bilibilias.android.koin)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias { libs.plugins.kotlin.parcelize }
    alias(libs.plugins.ksp)
}
val enabledPlayAppMode: String by project
val enabledAnalytics: String by project
fun Project.optionalStringProperty(name: String): String = findProperty(name)?.toString() ?: ""
val baiduStatId: String = project.optionalStringProperty("as.baidu.stat.id")
val githubOrg: String = project.optionalStringProperty("as.github.org")
val githubRepository: String = project.optionalStringProperty("as.github.repository")
// 打包时写入的源码提交哈希，固定取 7 位。
// 用途：安装包来源校验（用户在「我已知晓」之后必须输入正确的 7 位提交哈希才能进入应用），
// 以及首页「当前构建」标识。使用 --short=7 而不是 --short，避免在大型仓库中
// git 为了消歧返回更长的前缀，导致「只取前七位」的校验规则与产物不一致。
val gitCommitHash: String = providers.exec {
    commandLine("git", "rev-parse", "--short=7", "HEAD")
}.standardOutput.asText.get().trim()
val isDebugBuild = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("debug", ignoreCase = true)
}

android {
    namespace = "com.imcys.bilibilias"

    defaultConfig {
        applicationId = "com.imcys.bilibilias"
        versionCode = 320
        versionName = "320"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["BAIDU_STAT_ID"] = baiduStatId
        buildConfigField("String", "BAIDU_STAT_ID", """"$baiduStatId"""".trimIndent())
        buildConfigField("String", "GIT_COMMIT_HASH", """"$gitCommitHash"""".trimIndent())
        buildConfigField("String","GITHUB_ORG",""""$githubOrg"""".trimIndent())
        buildConfigField("String","GITHUB_REPOSITORY",""""$githubRepository"""".trimIndent())
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    lint {
        disable += "Instantiatable"
    }

    signingConfigs {
        create("BILIBILIASSigningConfig") {
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    flavorDimensions += listOf("version")
    productFlavors {

        create("official") {
            dimension = "version"
            buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", enabledPlayAppMode)
            signingConfig = signingConfigs.getByName("BILIBILIASSigningConfig")
            resValue("string", "app_channel", "Official")
        }

        create("alpha") {
            dimension = "version"
            applicationIdSuffix = BILIBILIASBuildType.ALPHA.applicationIdSuffix
            versionNameSuffix = BILIBILIASBuildType.ALPHA.versionNameSuffix
            buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", "false")
            resValue("string", "app_channel", "Alpha")
            // 动态签名配置
            val runnerTemp = System.getenv("RUNNER_TEMP")
            signingConfig = if (runnerTemp != null && file("$runnerTemp/mxjs-debug.jks").exists()) {
                // CI 环境
                signingConfigs.create("ci-alpha").apply {
                    storeFile = file("$runnerTemp/mxjs-debug.jks")
                    storePassword = System.getenv("ALPHA_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("ALPHA_KEY_ALIAS")
                    keyPassword = System.getenv("ALPHA_KEY_PASSWORD")
                    enableV3Signing = true
                    enableV4Signing = true
                }
            } else {
                // 本地环境
                signingConfigs.getByName("debug")
            }

        }

        // 提交Google Play使用
        create("beta") {
            dimension = "version"
            applicationIdSuffix = BILIBILIASBuildType.BETA.applicationIdSuffix
            versionNameSuffix = BILIBILIASBuildType.BETA.versionNameSuffix
            buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", enabledPlayAppMode)
            signingConfig = signingConfigs.getByName("BILIBILIASSigningConfig")
            resValue("string", "app_channel", "Beta")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", enabledPlayAppMode)
            buildConfigField("boolean", "ENABLED_ANALYTICS", enabledAnalytics)
        }

        debug {
            buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", enabledPlayAppMode)
            buildConfigField("boolean", "ENABLED_ANALYTICS", enabledAnalytics)
        }

    }

    splits {
        abi {
            isEnable = !isDebugBuild  // debug 时禁用，release 时启用
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.ffmpeg.kit.x6kb)

    implementation(libs.androidx.documentfile)

    // Native MCP
    implementation(libs.appfunctions)
    implementation(libs.appfunctions.service)
    ksp(libs.appfunctions.compiler)

    // 预览工具
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}

