package com.imcys.bilibilias.shared.platform.runtime

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import org.koin.mp.KoinPlatform.getKoin
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

val koinApplication: Context = getKoin().get<Application>()

actual fun openLink(url: String): Boolean {
    return try {
        koinApplication.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            )
        )
        true
    } catch (e: Exception) {
        false
    }
}

actual fun format(format: String, vararg args: Any?): String {
    return String.format(format, *args)
}

@Composable
actual fun rememberAppSignature(): String? {
    val context = LocalContext.current
    val packageName = context.packageName
    return remember(packageName, context) {
        try {
            // GET_SIGNATURES 在 API 33+ 已废弃，但为了兼容 Android 9 以下仍需使用，
            // 故在声明处集中抑制该废弃警告。
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                if (Build.VERSION.SDK_INT >= 28)
                    PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            )
            val signatures = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.getOrNull(0)?.toByteArray()
            if (cert != null) {
                val md = MessageDigest.getInstance("SHA1")
                val publicKey = md.digest(cert)
                publicKey.joinToString(":") { "%02X".format(it) }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

actual fun getAppVersion(): Pair<Long, String> {
    val context = koinApplication
    val pm = context.packageManager
    val pkg = context.packageName
    val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") pm.getPackageInfo(pkg, 0)
    }

    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pi.longVersionCode
    } else {
        @Suppress("DEPRECATION") pi.versionCode.toLong()
    }
    val name = pi.versionName ?: "unknown"

    return code to name
}

/**
 * Android 侧直接返回 `Build.VERSION.SDK_INT`，用于设置页按系统版本置灰能力项。
 */
actual fun getAndroidApiLevel(): Int = Build.VERSION.SDK_INT


actual fun urlEncode(url: String): String {
    return URLEncoder.encode(url, "UTF-8")
}

actual fun urlDecode(url: String): String {
    return URLDecoder.decode(url, "UTF-8")
}
