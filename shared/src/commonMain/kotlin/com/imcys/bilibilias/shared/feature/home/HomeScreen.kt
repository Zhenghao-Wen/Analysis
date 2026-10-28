package com.imcys.bilibilias.shared.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import bilibilias.shared.ASBuildConfig
import bilibilias.shared.generated.resources.Res
import bilibilias.shared.generated.resources.app_name
import bilibilias.shared.generated.resources.common_cancel
import bilibilias.shared.generated.resources.home_announcement
import bilibilias.shared.generated.resources.home_announcement_close_hint
import bilibilias.shared.generated.resources.home_close_announcement
import bilibilias.shared.generated.resources.ic_brand_awareness_24px
import bilibilias.shared.generated.resources.ic_info_24px
import bilibilias.shared.generated.resources.ic_logo_mini
import bilibilias.shared.generated.resources.update_content
import bilibilias.shared.generated.resources.update_go_download
import bilibilias.shared.generated.resources.update_hint_title
import bilibilias.shared.generated.resources.update_loading
import bilibilias.shared.generated.resources.update_new_version_format
import com.imcys.bilibilias.common.data.ASBuildType
import com.imcys.bilibilias.common.data.getASBuildType
import com.imcys.bilibilias.shared.util.ClipboardAutoHandler
import com.imcys.bilibilias.data.model.BILILoginUserModel
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.shared.download.model.AppDownloadTask
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.model.app.AppUpdateConfigInfo
import com.imcys.bilibilias.network.model.app.BulletinConfigInfo
import com.imcys.bilibilias.shared.platform.component.PlatformBackHandler
import com.imcys.bilibilias.shared.platform.runtime.getAppVersion
import com.imcys.bilibilias.shared.platform.runtime.openLink
import com.imcys.bilibilias.shared.platform.runtime.rememberAppSignature
import com.imcys.bilibilias.shared.feature.analysis.navigation.AnalysisRoute
import com.imcys.bilibilias.shared.feature.home.navigation.HomeRoute
import com.imcys.bilibilias.ui.utils.ASSizeClass
import com.imcys.bilibilias.ui.utils.rememberHeightSizeClass
import com.imcys.bilibilias.ui.utils.rememberWidthSizeClass
import com.imcys.bilibilias.ui.component.ASAlertDialog
import com.imcys.bilibilias.ui.component.ASAsyncImage
import com.imcys.bilibilias.ui.component.ASCardTextField
import com.imcys.bilibilias.ui.component.ASIconButton
import com.imcys.bilibilias.ui.component.ASTextButton
import com.imcys.bilibilias.ui.component.ASTopAppBar
import com.imcys.bilibilias.ui.component.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.component.SurfaceColorCard
import com.imcys.bilibilias.ui.component.tip.ASWarringTip
import com.imcys.bilibilias.ui.utils.DialogSortHost
import com.imcys.bilibilias.shared.ui.component.ASLoginPlatformFilterChipRow
import com.imcys.bilibilias.shared.ui.component.AsAutoError
import com.imcys.bilibilias.shared.ui.component.DownloadTaskCard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
)
@Composable
internal fun HomeScreen(
    homeRoute: HomeRoute,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    goToLogin: () -> Unit,
    goToUserPage: (mid: Long) -> Unit,
    goToAnalysis: () -> Unit,
    goToDownloadPage: () -> Unit,
    goToSetting: () -> Unit = {},
    goToPage: (NavKey) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val vm = koinViewModel<HomeViewModel>()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val loginUserInfoState by vm.loginUserInfoState.collectAsStateWithLifecycle()
    val userLoginPlatformList by vm.userLoginPlatformList.collectAsStateWithLifecycle()
    var popupUserInfoState by remember { mutableStateOf(false) }
    val windowHeightSizeClass = rememberHeightSizeClass()
    val downloadListState by vm.downloadListState.collectAsStateWithLifecycle()
    val appSettings by vm.appSettings.collectAsStateWithLifecycle(initialValue = AppSettings.getDefaultInstance())
    val windowsWidthSizeClass = rememberWidthSizeClass()


    ClipboardAutoHandler(
        appSettings = appSettings,
        shouldHandleClipboard = {
            appSettings.agreePrivacyPolicy != AppSettings.AgreePrivacyPolicyState.Default
        },
        onClipboardText = { text ->
            goToPage(AnalysisRoute(asInputText = text))
        },
    )


    LaunchedEffect(homeRoute.isFormLogin) {
        if (homeRoute.isFormLogin && !uiState.fromLoginEventConsumed) {
            vm.onNavigatedFromLogin()
            popupUserInfoState = true
        }
    }

    val homeLayoutTypesetList by vm.homeLayoutTypesetList.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val pagerScope = rememberCoroutineScope()
    PlatformBackHandler(enabled = pagerState.currentPage > 0) {
        pagerScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    HomeScaffold(
        snackbarHostState = snackbarHostState,
        loginUserInfoState,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        goToLogin = goToLogin,
        goToUserPage = { goToUserPage.invoke(loginUserInfoState.data?.mid ?: 0L) },
        goToAnalysis = goToAnalysis,
        goToSetting = goToSetting
    ) { p ->

        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 内容区
            HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> {
                            HomeContent(
                                homeLayoutTypesetList,
                                downloadListState,
                                goToDownloadPage,
                                goToPage
                            )
                        }

                        1 -> {
                            ToolsScreen(
                                onToPage = goToPage,
                                onUpdateUseToolRecord = vm::updateUseToolRecord
                            )
                        }
                    }
                }
            }

            // 分页指示器
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(2) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .padding(2.dp)
                            .size(10.dp, 6.dp)
                            .alpha(if (isSelected) 1f else 0.5f)
                            .then(
                                if (isSelected) {
                                    Modifier
                                } else {
                                    Modifier.alpha(0.5f)
                                }
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                    )
                }
            }

            // 底部输入区
            with(sharedTransitionScope) {
                AnimatedContent(windowHeightSizeClass) {
                    when (it) {
                        ASSizeClass.COMPACT -> {}
                        ASSizeClass.MEDIUM, ASSizeClass.EXPANDED -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    onClick = goToAnalysis,
                                    shape = CardDefaults.shape,
                                    modifier = Modifier.animateContentSize()
                                ) {
                                    ASCardTextField(
                                        modifier = Modifier
                                            .then(
                                                if (windowsWidthSizeClass != ASSizeClass.COMPACT &&
                                                    pagerState.currentPage == 0
                                                ) Modifier.fillMaxWidth(0.8f)
                                                else Modifier.fillMaxWidth()
                                            )
                                            .sharedElement(
                                                sharedTransitionScope.rememberSharedContentState(
                                                    key = "card-input-analysis"
                                                ),
                                                animatedVisibilityScope = animatedContentScope
                                            ),
                                        value = "",
                                        onValueChange = {},
                                        enabled = false,
                                        readOnly = true,
                                        leadingIcon = {
                                            Icon(
                                                modifier = Modifier.sharedElement(
                                                    sharedTransitionScope.rememberSharedContentState(
                                                        key = "icon-input-analysis"
                                                    ),
                                                    animatedVisibilityScope = animatedContentScope
                                                ),
                                                imageVector = Icons.Outlined.Search,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    LoginInfoBottomDialog(popupUserInfoState, loginUserInfoState, userLoginPlatformList) {
        popupUserInfoState = false
    }
}


/**
 * 首页内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    homeLayoutTypesetList: List<AppSettings.HomeLayoutItem>,
    downloadListState: List<AppDownloadTask>,
    goToDownloadPage: () -> Unit,
    goToPage: (NavKey) -> Unit
) {
    val vm = koinViewModel<HomeViewModel>()

    val bulletinInfo by vm.bulletinInfo.collectAsStateWithLifecycle()
    val appSettings by vm.appSettingsState.collectAsStateWithLifecycle()
    val appUpdateInfo by vm.appUpdateInfo.collectAsStateWithLifecycle()
    val useToolHistoryList by vm.useToolHistoryList.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val windowsWidthSizeClass = rememberWidthSizeClass()
    val windowHeightSizeClass = rememberHeightSizeClass()
    val toolsHistoryCount = when (windowsWidthSizeClass) {
        ASSizeClass.COMPACT -> 2
        ASSizeClass.MEDIUM if windowHeightSizeClass != ASSizeClass.COMPACT -> 4
        ASSizeClass.EXPANDED if windowHeightSizeClass != ASSizeClass.COMPACT -> 6
        else -> 4
    }

    var closeBulletinDialogShow by remember { mutableStateOf(false) }
    var bulletinDialogShow by remember { mutableStateOf(false) }
    var unknownAppSign by remember { mutableStateOf(false) }
    var packageSourceWarningDialogShow by remember { mutableStateOf(false) }

    // 安装包来源校验：点击「我已知晓」后进入，必须输入与打包时一致的 7 位提交哈希。
    // 通过后才会记录「当前构建已确认」；否则每次启动都会重新要求校验。
    var packageSourceVerifyDialogShow by remember { mutableStateOf(false) }
    val buildCommitHash = remember { vm.buildCommitHash() }
    val shouldShowUnknownAppSignWarning =
        remember(unknownAppSign, appSettings.unknownAppSignWarningCloseTime) {
            val oneMonthAgo =
                Clock.System.now() - HomeViewModel.UNKNOWN_APP_SIGN_WARNING_HIDE_DURATION_MS
            unknownAppSign && Instant.fromEpochMilliseconds(appSettings.unknownAppSignWarningCloseTime) <= oneMonthAgo
        }

    // 签名校验已停用：本项目已停止分发，不存在可供对照的「官方包」，
    // 因此不再用硬编码的 SHA1 去判定当前安装包是否被篡改，
    // 也就不会再出现「当前应用签名未知」的横幅提示。
    // 保留下方 unknownAppSign / shouldShowUnknownAppSignWarning 及对应 UI 代码以便恢复：
    // 由于 unknownAppSign 恒为 false，该横幅现在恒定不展示。
    //
    // val currentSHA1 = rememberAppSignature()
    // LaunchedEffect(currentSHA1) {
    //     if (currentSHA1 == null || !checkSign(currentSHA1)) {
    //         unknownAppSign = true
    //     }
    // }

    LaunchedEffect(Unit) {
        vm.initOldAppInfo()
    }

    val packageSourceWarningKey = remember { vm.getPackageSourceWarningKey() }

    // 每个构建只弹一次：判定依据是「上次记录的版本号+提交哈希」与当前安装包是否一致。
    // 必须直接读持久化值（见 needsPackageSourceVerification 注释），
    // 不能用 appSettings 的 StateFlow 首帧判断，否则会每次启动都弹。
    LaunchedEffect(packageSourceWarningKey) {
        if (vm.needsPackageSourceVerification()) {
            packageSourceWarningDialogShow = true
            vm.markPackageSourceWarningShownThisLaunch()
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(
            Modifier
                .padding(horizontal = 15.dp)
                .padding(top = 10.dp)
                .then(
                    if (windowsWidthSizeClass != ASSizeClass.COMPACT) Modifier.fillMaxWidth(
                        0.8f
                    ) else Modifier
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (shouldShowUnknownAppSignWarning) {
                item {
                    ASWarringTip(
                        Modifier
                            .animateItem()
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (false) {
                                    "当前App处于Debug模式，如果您并非开发人员，请谨慎使用。"
                                } else {
                                    "当前应用签名未知，请谨慎使用！"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ASIconButton(
                                onClick = vm::closeUnknownAppSignWarning,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "关闭"
                                )
                            }
                        }
                    }
                }
            }

            homeLayoutTypesetList.forEach { layout ->
                if (!layout.isHidden) {
                    when (layout.type) {

                        AppSettings.HomeLayoutType.DownloadList -> {
                            item {
                                DownloadListCard(
                                    modifier = Modifier
                                        .animateItem()
                                        .animateContentSize(),
                                    downloadListState,
                                    goToDownloadPage = goToDownloadPage,
                                    onPauseTask = {
                                        vm.pauseDownloadTask(it.downloadSegment.segmentId)
                                    },
                                    onResumeTask = {
                                        vm.resumeDownloadTask(it.downloadSegment.segmentId)
                                    },
                                    onCancelTask = {
                                        vm.cancelDownloadTask(it.downloadSegment.segmentId)
                                    }
                                )
                            }
                        }

                        AppSettings.HomeLayoutType.Tools -> {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    val allToolCount by remember(
                                        toolsHistoryCount,
                                        useToolHistoryList.size
                                    ) {
                                        mutableIntStateOf(
                                            min(
                                                toolsHistoryCount,
                                                useToolHistoryList.size
                                            )
                                        )
                                    }
                                    useToolHistoryList.take(allToolCount)
                                        .forEachIndexed { index, tool ->
                                            key(tool.title) {
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(18.3f / 13f),
                                                    shape = CardDefaults.shape,
                                                    color = if ((index + 1) % 2 != 0) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.tertiaryContainer,
                                                    onClick = {
                                                        tool.let { vm.updateUseToolRecord(it) }
                                                        goToPage(tool.navKey)
                                                    }
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                            )
                                                    ) {
                                                        tool.icon?.let {
                                                            Icon(
                                                                it,
                                                                contentDescription = "图标",
                                                                modifier = Modifier
                                                            )
                                                        } ?: run {
                                                            Icon(
                                                                painter = painterResource(tool.iconRes!!),
                                                                contentDescription = "图标",
                                                                modifier = Modifier
                                                            )
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(tool.title, fontSize = 22.sp)
                                                        Spacer(Modifier.weight(1f))
                                                        Row(
                                                            Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Icon(
                                                                Icons.AutoMirrored.Outlined.ArrowForward,
                                                                contentDescription = "前往"
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    repeat((toolsHistoryCount - allToolCount)) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }


                            }
                        }

                        else -> {}
                    }
                }
            }

            item {
                Text(
                    "请在Download/BILIBILIAS目录下查看下载内容",
                    fontSize = 14.sp,
                    fontWeight = FontWeight(330),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }


    DialogSortHost {
        // 安装包来源提示 → 「我已知晓」→ 提交哈希校验，两步都不可点击空白关闭。
        // DialogSortHost 只会渲染 order 最小的可见对话框，因此两者的 visible 互斥。
        dialog(
            visible = packageSourceWarningDialogShow && !packageSourceVerifyDialogShow,
            order = 0,
            key = "package_source_warning"
        ) {
            PackageSourceWarningDialog(
                onClickConfirm = {
                    // 强化点：确认不再是终点，而是进入校验
                    packageSourceVerifyDialogShow = true
                }
            )
        }

        dialog(
            visible = packageSourceVerifyDialogShow,
            order = -1,
            key = "package_source_verify"
        ) {
            PackageSourceVerifyDialog(
                buildCommitHash = buildCommitHash,
                onSubmit = { input -> vm.verifyBuildCommitHash(input) },
                onVerified = {
                    // 记录「版本号+提交哈希」；升级或换提交构建后会再次要求校验
                    vm.markPackageSourceVerifiedForCurrentBuild()
                    packageSourceVerifyDialogShow = false
                    packageSourceWarningDialogShow = false
                },
                onBack = {
                    packageSourceVerifyDialogShow = false
                    packageSourceWarningDialogShow = true
                }
            )
        }

        dialog(
            visible = !packageSourceWarningDialogShow && !packageSourceVerifyDialogShow,
            order = 1,
            key = "update_app"
        ) {
            UpdateAppDialog(appUpdateInfo, uiState.shownAppUpdate, onAppUpdateDialogShown = {
                vm.onAppUpdateDialogShown()
            })
        }

        dialog(
            visible = !packageSourceWarningDialogShow && !packageSourceVerifyDialogShow && closeBulletinDialogShow,
            order = 2,
            key = "close_bulletin"
        ) {
            CloseBulletinDialog(closeBulletinDialogShow, onClickConfirm = {
                vm.updateLastBulletinContent()
            }, onClickDismiss = {
                closeBulletinDialogShow = false
            })
        }

        dialog(
            visible = !packageSourceWarningDialogShow && !packageSourceVerifyDialogShow && bulletinDialogShow,
            order = 3,
            key = "bulletin"
        ) {
            BulletinDialog(bulletinInfo, bulletinDialogShow, onClickConfirm = {
                bulletinDialogShow = false
            })
        }
    }
}

/**
 * 安装包来源提示（第一步）。
 *
 * 相比旧版的分支：
 * - 主按钮文案改为「我已知晓」，且点击后**不是**关闭流程，而是进入第二步的提交哈希校验；
 * - 去掉了「此版本不再提示」按钮 —— 它会绕过校验，让来源校验形同虚设；
 * - `clickBlankDismiss = false`，避免点击空白直接跳过。
 *
 * 之所以保留这一步：它承担合规与风险提示的告知作用，用户必须显式确认后才能进入校验。
 */
@Composable
private fun PackageSourceWarningDialog(
    onClickConfirm: () -> Unit,
) {
    ASAlertDialog(
        showState = true,
        clickBlankDismiss = false,
        title = { Text("注意") },
        text = {
            Text(
                "该程序由非开源仓库开发者打包而来，这是因为本项目已经停止分发。使用即代表你已同意带来的一切后果自负，并不会在国内主流平台传播。除非你是学习本项目的开发者，否则你目前使用的APP有可能遭遇篡改。"
            )
        },
        confirmButton = {
            ASTextButton(onClick = onClickConfirm) {
                Text(text = "我已知晓")
            }
        },
    )
}

/**
 * 安装包来源校验（第二步，沿袭全局 MD3 主题）。
 *
 * 要求用户输入**本安装包对应源码提交的 7 位哈希**，与打包时通过
 * `git rev-parse --short=7 HEAD` 写入 `ASBuildConfig.GIT_COMMIT_HASH` 的值比对，
 * 一致才允许进入应用。
 *
 * 这样无论是拉取源码本地编译，还是私有仓库/CI 构建出的安装包，
 * 都能追溯到具体的源码仓库提交：只要从源码仓库找到对应提交即可比对。
 *
 * **严格模式（宁可锁死也不放行）**：
 * 本项目面向开发者群体，因此不提供任何绕过路径。
 * - 输入容错：忽略大小写、前后空格、可带 `#` 前缀，只比较前 7 位；
 * - 若构建产物中**没有**提交信息（源码压缩包编译、`.git` 缺失等），
 *   则校验无法完成，**直接锁定**：输入框禁用、无「返回」按钮、无法继续，
 *   并提示用户从源码仓库 `git clone` 后重新编译；
 * - 两个按钮均无绕过入口，`clickBlankDismiss = false` 同时挡住点击空白与返回键
 *   （Material3 的 AlertDialog 中返回键同样走 `onDismissRequest`）。
 */
@Composable
private fun PackageSourceVerifyDialog(
    buildCommitHash: String,
    onSubmit: (String) -> Boolean,
    onVerified: () -> Unit,
    onBack: () -> Unit,
) {
    val hasBuildCommitHash = buildCommitHash.isNotBlank()
    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ASAlertDialog(
        showState = true,
        clickBlankDismiss = false,
        title = { Text("验证安装包来源") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (hasBuildCommitHash) {
                    Text(
                        "为防止安装包被篡改，请输入本安装包对应源码提交的 7 位哈希值。"
                    )
                    // 醒目的警告块：用 errorContainer 底色 + onErrorContainer 文字，
                    // 字号与上文一致（bodyMedium），跟随全局 MD3 主题。
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "如果你不知道要输入的东西有什么意义，或者这7位是别人告诉你的，那你应该放弃使用该应用。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "获取方式：在源码仓库的提交记录中查看当前版本对应的提交，只取前七位；" +
                            "若为自行编译，可执行 git rev-parse --short=7 HEAD。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage != null,
                        label = { Text("提交哈希（7 位）") },
                        placeholder = { Text("例如 1a2b3c4") },
                    )
                    errorMessage?.let { message ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // 严格模式：没有构建期提交信息 → 无法验证 → 锁定，不放行
                    Text(
                        text = "当前安装包不包含构建期提交信息，无法验证来源，因此不能进入应用。",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "可能原因：使用源码压缩包编译，或编译环境缺少 .git 目录，" +
                            "导致打包时拿不到提交哈希。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "解决方式：从源码仓库 git clone 完整仓库后重新编译安装。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (hasBuildCommitHash) {
                ASTextButton(
                    onClick = {
                        if (onSubmit(input)) {
                            onVerified()
                        } else {
                            errorMessage = "哈希不匹配。请确认安装包来源与源码提交是否一致。"
                        }
                    }
                ) {
                    Text(text = "验证")
                }
            } else {
                // 无可用校验值：按钮禁用，且不提供返回/跳过入口
                ASTextButton(onClick = {}, enabled = false) {
                    Text(text = "无法继续")
                }
            }
        },
        dismissButton = if (hasBuildCommitHash) {
            {
                ASTextButton(onClick = onBack) {
                    Text(text = "返回")
                }
            }
        } else {
            null
        },
    )
}

@Composable
fun UpdateAppDialog(
    appUpdateInfo: AppUpdateConfigInfo?,
    shownAppUpdate: Boolean,
    onAppUpdateDialogShown: () -> Unit
) {
    var show by remember { mutableStateOf(false) }

    if (ASBuildConfig.ENABLED_PLAY_APP_MODE) return
    if (appUpdateInfo == null) return

    LaunchedEffect(Unit) {
        if (appUpdateInfo.version != getAppVersion().second && !shownAppUpdate) {
            show = true
            onAppUpdateDialogShown()
        }
    }
    ASAlertDialog(
        showState = show,
        title = { Text(stringResource(Res.string.update_hint_title)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(Res.string.update_new_version_format, appUpdateInfo.version))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.update_content))
                Spacer(Modifier.height(4.dp))
                Text(appUpdateInfo.feat)
            }
        },
        confirmButton = {
            ASTextButton(onClick = {
                openLink(appUpdateInfo.url)
            }) {
                Text(text = stringResource(Res.string.update_go_download))
            }
        }, dismissButton = {
            ASTextButton(onClick = {
                show = false
            }) {
                Text(text = stringResource(Res.string.common_cancel))
            }
        }, onDismiss = {
            show = false
        })
}

@Composable
private fun CloseBulletinDialog(
    show: Boolean, onClickConfirm: () -> Unit, onClickDismiss: () -> Unit
) {
    ASAlertDialog(
        showState = show,
        title = { Text(stringResource(Res.string.home_close_announcement)) },
        text = {
            Text(stringResource(Res.string.home_announcement_close_hint))
        },
        confirmButton = {
            ASTextButton(onClick = onClickConfirm) {
                Text(text = "确认")
            }
        },
        dismissButton = {
            ASTextButton(onClick = onClickDismiss) {
                Text(text = "取消")
            }
        }
    )
}


@Composable
private fun BulletinDialog(
    bulletinConfigInfo: BulletinConfigInfo?,
    show: Boolean, onClickConfirm: () -> Unit,
) {
    ASAlertDialog(
        showState = show,
        title = { Text(stringResource(Res.string.home_announcement)) },
        text = {
            Column(
                modifier = Modifier
            ) {
                Text(bulletinConfigInfo?.content ?: "")
            }
        },
        confirmButton = {
            ASTextButton(onClick = onClickConfirm) {
                Text(text = "确认")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadListCard(
    modifier: Modifier = Modifier,
    downloadListState: List<AppDownloadTask>,
    goToDownloadPage: () -> Unit,
    onPauseTask: (task: AppDownloadTask) -> Unit,
    onResumeTask: (task: AppDownloadTask) -> Unit,
    onCancelTask: (task: AppDownloadTask) -> Unit,
) {
    SurfaceColorCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = "下载列表图标",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(0.72f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "下载列表",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier.alpha(0.72f),
                )
                Spacer(Modifier.weight(1f))
                ASIconButton(onClick = {
                    goToDownloadPage.invoke()
                }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "下载详情列表"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (downloadListState.isNotEmpty()) {
                    downloadListState.subList(0, min(3, downloadListState.size))
                        .forEach { task ->
                            DownloadTaskCard(task = task, onPause = {
                                onPauseTask(task)
                            }, onResume = {
                                onResumeTask(task)
                            }, onCancel = {
                                onCancelTask(task)
                            })
                        }
                } else {
                    Text(
                        "暂无缓存任务",
                        modifier = Modifier.alpha(0.72f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight(330),
                    )
                }
            }
        }
    }
}


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
private fun HomeScaffold(
    snackbarHostState: SnackbarHostState,
    loginUserInfoState: NetWorkResult<BILILoginUserModel?>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    goToLogin: () -> Unit,
    goToUserPage: () -> Unit,
    goToAnalysis: () -> Unit,
    goToSetting: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val windowHeightSizeClass = rememberHeightSizeClass()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Column {
                ASTopAppBar(
                    style = BILIBILIASTopAppBarStyle.Small,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(15.dp))
                            Icon(
                                painterResource(Res.drawable.ic_logo_mini),
                                contentDescription = stringResource(Res.string.app_name),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .width(29.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.W500,
                                color = MaterialTheme.colorScheme.onSurface,
                                text = stringResource(Res.string.app_name)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    navigationIcon = {},
                    actions = {
                        AsAutoError(
                            loginUserInfoState, onSuccessContent = {
                                ASAsyncImage(
                                    model = loginUserInfoState.data?.face,
                                    modifier = Modifier
                                        .size(40.dp),
                                    shape = CircleShape,
                                    contentDescription = "头像",
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        goToUserPage()
                                    }
                                )
                            },
                            onLoadingContent = {
                                ContainedLoadingIndicator()
                            },
                            onDefaultContent = {
                                Row {
                                    ASIconButton(onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        goToLogin()
                                    }) {
                                        Icon(
                                            Icons.Outlined.AccountCircle,
                                            contentDescription = "登录"
                                        )
                                    }
                                    Spacer(Modifier.width(2.dp))
                                    ASIconButton(onClick = goToSetting) {
                                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                                    }
                                }
                            },
                            onErrorContent = { _, _ ->
                                ASIconButton(onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    goToLogin()
                                }) {
                                    Icon(
                                        Icons.Outlined.AccountCircle,
                                        contentDescription = "登录"
                                    )
                                }
                            })
                        Spacer(Modifier.width(15.dp))
                    }
                )
            }
        },
        floatingActionButton = {
            with(sharedTransitionScope) {
                AnimatedContent(windowHeightSizeClass) {
                    when (it) {
                        ASSizeClass.COMPACT -> {
                            FloatingActionButton(
                                onClick = goToAnalysis,
                                modifier = Modifier.sharedElement(
                                    sharedTransitionScope.rememberSharedContentState(
                                        key = "card-input-analysis"
                                    ),
                                    animatedVisibilityScope = animatedContentScope,
                                ),
                                containerColor = MaterialTheme.colorScheme.primary,
                            ) {
                                Icon(
                                    modifier = Modifier.sharedElement(
                                        sharedTransitionScope.rememberSharedContentState(
                                            key = "icon-input-analysis"
                                        ),
                                        animatedVisibilityScope = animatedContentScope
                                    ),
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "视频解析",
                                )
                            }
                        }

                        ASSizeClass.MEDIUM, ASSizeClass.EXPANDED -> {}
                    }
                }
            }
        }
    ) {
        content.invoke(it)
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun LoginInfoBottomDialog(
    popup: Boolean,
    loginUserInfoState: NetWorkResult<BILILoginUserModel?>,
    userLoginPlatformList: List<BILIUsersEntity>,
    onDismissRequest: () -> Unit,
) {
    if (popup) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(10.dp),

                ) {
                AsAutoError(loginUserInfoState, onSuccessContent = {
                    Column(
                        Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ASAsyncImage(
                            model = loginUserInfoState.data?.face,
                            contentDescription = "头像",
                            modifier = Modifier.size(100.dp),
                            shape = MaterialShapes.Square.toShape()
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(loginUserInfoState.data?.name ?: "", fontSize = 20.sp)
                        ASLoginPlatformFilterChipRow(userLoginPlatformList.map { it.loginPlatform })
                    }
                }, onLoadingContent = {
                    Column(
                        Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ContainedLoadingIndicator(Modifier.size(100.dp))
                        Text(stringResource(Res.string.update_loading))
                    }
                })
            }
        }
    }
}


private fun checkSign(actual: String?): Boolean {
    val officeSign = "8E:B3:80:FB:C0:32:86:98:5B:8F:86:59:B2:79:16:75:A0:AB:21:DB"
    val officeAlphaSign = "7F:44:47:60:4B:BF:FB:A8:06:FD:13:DF:7F:E3:5D:AA:70:4B:D5:54"
    return isSignatureSHA1Match(actual, officeSign) ||
            isSignatureSHA1Match(actual, officeAlphaSign)
}

private fun isSignatureSHA1Match(actual: String?, expected: String): Boolean {
    return actual?.equals(expected, ignoreCase = true) == true
}
