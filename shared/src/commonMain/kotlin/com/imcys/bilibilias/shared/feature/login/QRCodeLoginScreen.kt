package com.imcys.bilibilias.shared.feature.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import bilibilias.shared.generated.resources.Res
import bilibilias.shared.generated.resources.cd_back
import bilibilias.shared.generated.resources.cd_download_qrcode
import bilibilias.shared.generated.resources.cd_go_scan
import bilibilias.shared.generated.resources.cd_login_qrcode
import bilibilias.shared.generated.resources.cd_more_actions
import bilibilias.shared.generated.resources.cd_not_agree_privacy
import bilibilias.shared.generated.resources.cd_tv_scan
import bilibilias.shared.generated.resources.cd_web_scan
import bilibilias.shared.generated.resources.common_retry
import bilibilias.shared.generated.resources.login_check_privacy_policy
import bilibilias.shared.generated.resources.login_download_qrcode
import bilibilias.shared.generated.resources.login_go_scan
import bilibilias.shared.generated.resources.login_network_error
import bilibilias.shared.generated.resources.login_qrcode_title
import bilibilias.shared.generated.resources.login_tv
import bilibilias.shared.generated.resources.login_use_cookie
import bilibilias.shared.generated.resources.login_web
import com.imcys.bilibilias.database.entity.LoginPlatform
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.model.QRCodeInfo
import com.imcys.bilibilias.shared.platform.firebase.FirebaseExt
import com.imcys.bilibilias.shared.feature.login.navigation.QRCodeLoginRoute
import com.imcys.bilibilias.ui.utils.ASSizeClass
import com.imcys.bilibilias.ui.utils.rememberWidthSizeClass
import com.imcys.bilibilias.ui.component.ASIconButton
import com.imcys.bilibilias.ui.component.ASTopAppBar
import com.imcys.bilibilias.ui.component.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.component.tip.ASWarringTip
import com.imcys.bilibilias.shared.ui.component.ASAgreePrivacyPolicy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun QRCodeLoginScreen(
    route: QRCodeLoginRoute,
    onToBack: () -> Unit,
    onBackHomePage: () -> Unit,
    onToCookieLogin: () -> Unit
) {
    val vm = koinViewModel<QRCodeLoginViewModel>()
    val uiState = vm.uiState
    val qrCodeInfoState by vm.qrCodeInfoState.collectAsStateWithLifecycle()
    val qrCodeScanInfoState by vm.qrCodeScanInfoState.collectAsStateWithLifecycle()
    val loginUserInfoState by vm.loginUserInfoState.collectAsStateWithLifecycle()
    val windowWidthSizeClass = rememberWidthSizeClass()
    var agreePrivacyPolicy by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(route.defaultLoginPlatform) {
        vm.updateLoginPlatform(route.defaultLoginPlatform)
    }

    // 监听扫码结果
    LaunchedEffect(qrCodeScanInfoState) {
        when (qrCodeScanInfoState) {
            is NetWorkResult.Success<*> -> {
                when (qrCodeScanInfoState.data?.code) {
                    0 -> {
                        vm.getLoginUserInfo(
                            qrCodeScanInfoState.data
                        )
                    }

                    86038 -> {
                        // 二维码失效
                        vm.getLoadLoginQRCodeInfo()
                    }

                    else -> {
                    }
                }
            }

            else -> {}
        }
    }

    // 监听登录信息获取
    LaunchedEffect(loginUserInfoState) {
        when (loginUserInfoState) {
            is NetWorkResult.Success<*> -> {
                vm.saveLoginInfo(loginUserInfoState.data) {
                    FirebaseExt.logLogin("QRCode")
                    if (route.isFromRoam || route.isFromAnalysis) {
                        // 如果是从漫游页面进入的，登录成功后直接返回
                        onToBack()
                    } else {
                        // 否则返回首页
                        onBackHomePage()
                    }
                }
            }

            else -> {}
        }
    }

    QRLoginScaffold(onToBack, onToCookieLogin) {
        Column(
            Modifier
                .padding(it)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            SharedTransitionLayout {
                AnimatedContent(windowWidthSizeClass, label = "扫码登录内容区域") { size ->
                    when (size) {
                        ASSizeClass.COMPACT -> {
                            QRCodeLoginContentWidthCompact(
                                route,
                                uiState,
                                qrCodeInfoState,
                                agreePrivacyPolicy,
                                updateLoginPlatform = vm::updateLoginPlatform,
                                updateQrCode = vm::getLoadLoginQRCodeInfo,
                                saveQRCodeImage = {  vm.saveQRCodeImageToGallery() },
                                goScanQR = { vm.goToScanQR() },
                                updateAgreePrivacyPolicy = { state ->
                                    agreePrivacyPolicy = state
                                },
                                animatedVisibilityScope = this@AnimatedContent,
                                sharedTransitionScope = this@SharedTransitionLayout
                            )
                        }

                        ASSizeClass.MEDIUM, ASSizeClass.EXPANDED -> {
                            QRCodeLoginContentWidthMediumAndExpanded(
                                route,
                                uiState,
                                qrCodeInfoState,
                                agreePrivacyPolicy,
                                updateLoginPlatform = vm::updateLoginPlatform,
                                updateQrCode = vm::getLoadLoginQRCodeInfo,
                                saveQRCodeImage = {  vm.saveQRCodeImageToGallery() },
                                goScanQR = { vm.goToScanQR() },
                                updateAgreePrivacyPolicy = { state ->
                                    agreePrivacyPolicy = state
                                },
                                animatedVisibilityScope = this@AnimatedContent,
                                sharedTransitionScope = this@SharedTransitionLayout
                            )
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QRCodeLoginContentWidthMediumAndExpanded(
    route: QRCodeLoginRoute,
    uiState: QRCodeLoginViewModel.UIState,
    qrCodeInfoState: NetWorkResult<QRCodeInfo?>,
    agreePrivacyPolicy: Boolean,
    updateLoginPlatform: (LoginPlatform) -> Unit,
    updateQrCode: () -> Unit,
    saveQRCodeImage: () -> Unit,
    goScanQR: () -> Unit,
    updateAgreePrivacyPolicy: (Boolean) -> Unit,
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    Row(Modifier.fillMaxWidth()) {
        with(sharedTransitionScope) {
            // QR内容区域
            QRCodeContent(
                Modifier
                    .weight(6f)
                    .fillMaxHeight()
                    .sharedElement(
                        rememberSharedContentState(key = "qrCodeContent"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                route,
                uiState.selectedLoginPlatform,
                qrCodeInfoState,
                agreePrivacyPolicy,
                updateLoginPlatform = updateLoginPlatform,
                updateQrCode = updateQrCode,
                updateAgreePrivacyPolicy = { state -> updateAgreePrivacyPolicy(state) }
            )
            // 操作区域
            Box(
                Modifier
                    .weight(4f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                ActionButton(
                    Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            rememberSharedContentState(key = "actionButton"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    agreePrivacyPolicy,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    saveQRCodeImage = {
                        saveQRCodeImage()
                    },
                    goScanQR = {
                        goScanQR()
                    },
                )
            }

        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QRCodeLoginContentWidthCompact(
    route: QRCodeLoginRoute,
    uiState: QRCodeLoginViewModel.UIState,
    qrCodeInfoState: NetWorkResult<QRCodeInfo?>,
    agreePrivacyPolicy: Boolean,
    updateLoginPlatform: (LoginPlatform) -> Unit,
    updateQrCode: () -> Unit,
    saveQRCodeImage: () -> Unit,
    goScanQR: () -> Unit,
    updateAgreePrivacyPolicy: (Boolean) -> Unit,
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    Column(Modifier.fillMaxWidth()) {
        with(sharedTransitionScope) {
            // QR内容区域
            QRCodeContent(
                Modifier
                    .weight(1f)
                    .sharedElement(
                        rememberSharedContentState(key = "qrCodeContent"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                route,
                uiState.selectedLoginPlatform,
                qrCodeInfoState,
                agreePrivacyPolicy,
                updateLoginPlatform = updateLoginPlatform,
                updateQrCode = updateQrCode,
                updateAgreePrivacyPolicy = { state -> updateAgreePrivacyPolicy(state) }
            )
            // 操作区域
            ActionButton(
                Modifier
                    .fillMaxWidth()
                    .sharedElement(
                        rememberSharedContentState(key = "actionButton"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                agreePrivacyPolicy,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope,
                saveQRCodeImage = {
                    saveQRCodeImage()
                },
                goScanQR = {
                    goScanQR()
                }
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QRLoginScaffold(
    onToBack: () -> Unit,
    onToCookieLogin: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column {
                ASTopAppBar(
                    style = BILIBILIASTopAppBarStyle.Small,
                    title = {
                        Text(stringResource(Res.string.login_qrcode_title))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    navigationIcon = {
                        ASIconButton(onClick = {
                            onToBack.invoke()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back)
                            )
                        }
                    },
                    actions = {
                        ASIconButton(onClick = {
                            expandedMenu = !expandedMenu
                        }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = stringResource(Res.string.cd_more_actions)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.login_use_cookie)) },
                                onClick = {
                                    expandedMenu = false
                                    onToCookieLogin.invoke()
                                }
                            )
                        }
                    }
                )
            }
        },
    ) {
        content.invoke(it)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ActionButton(
    modifier: Modifier,
    agreePrivacyPolicy: Boolean,
    saveQRCodeImage: () -> Unit,
    goScanQR: () -> Unit,
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    val windowWidthSizeClass = rememberWidthSizeClass()
    with(sharedTransitionScope) {
        AnimatedContent(windowWidthSizeClass) { size ->
            when (size) {
                ASSizeClass.COMPACT -> {
                    Row(modifier) {
                        Button(
                            enabled = agreePrivacyPolicy,
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "downloadQRButton"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer),
                            shape = CardDefaults.shape,
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            onClick = { saveQRCodeImage() }
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = stringResource(Res.string.cd_download_qrcode),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                stringResource(Res.string.login_download_qrcode),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(24.dp))
                        Button(
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "goToScanQRButton"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer),
                            shape = CardDefaults.shape,
                            onClick = { goScanQR() },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = stringResource(Res.string.cd_go_scan),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                stringResource(Res.string.login_go_scan),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                ASSizeClass.MEDIUM, ASSizeClass.EXPANDED -> {
                    Column(modifier) {
                        Button(
                            enabled = agreePrivacyPolicy,
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "downloadQRButton"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer),
                            shape = CardDefaults.shape,
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            onClick = { saveQRCodeImage() }
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = stringResource(Res.string.cd_download_qrcode),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                stringResource(Res.string.login_download_qrcode),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "goToScanQRButton"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer),
                            shape = CardDefaults.shape,
                            onClick = { goScanQR() },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = stringResource(Res.string.cd_go_scan),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                stringResource(Res.string.login_go_scan),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlatformToggleButton(
    route: QRCodeLoginRoute,
    selectedLoginPlatform: LoginPlatform,
    updateLoginPlatform: (LoginPlatform) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    ToggleButton(
        checked = selectedLoginPlatform == LoginPlatform.WEB,
        onCheckedChange = {
            if (it) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
            updateLoginPlatform(LoginPlatform.WEB)
        },
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = Icons.Outlined.WebAsset,
            contentDescription = stringResource(Res.string.cd_web_scan),
            modifier = Modifier.size(FilterChipDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(Res.string.login_web))
    }

    BadgedBox(
        badge = {
            Badge {
                Text("Beta")
            }
        }
    ) {
        ToggleButton(
            checked = selectedLoginPlatform == LoginPlatform.TV,
            onCheckedChange = {
                if (it) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                updateLoginPlatform(LoginPlatform.TV)
            },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                imageVector = Icons.Outlined.Tv,
                contentDescription = stringResource(Res.string.cd_tv_scan),
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(Res.string.login_tv))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QRCodeContent(
    modifier: Modifier = Modifier,
    route: QRCodeLoginRoute,
    selectedLoginPlatform: LoginPlatform,
    qrCodeInfoState: NetWorkResult<QRCodeInfo?>,
    agreePrivacyPolicy: Boolean,
    updateLoginPlatform: (LoginPlatform) -> Unit,
    updateQrCode: () -> Unit,
    updateAgreePrivacyPolicy: (Boolean) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val widthSizeClass = rememberWidthSizeClass()
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val modifier = when (widthSizeClass) {
                ASSizeClass.COMPACT -> Modifier.fillMaxWidth(0.6f)
                ASSizeClass.MEDIUM, ASSizeClass.EXPANDED -> Modifier.fillMaxHeight(
                    0.6f
                )
            }.aspectRatio(1f)

            Surface(
                modifier = modifier,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = CardDefaults.shape,
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(agreePrivacyPolicy) {
                        if (it) {
                            when (qrCodeInfoState.status) {
                                ApiStatus.ERROR -> {
                                    ASIconButton(onClick = {
                                        updateQrCode()
                                    }) {
                                        Icon(
                                            Icons.Outlined.Replay,
                                            contentDescription = stringResource(Res.string.common_retry)
                                        )
                                    }
                                    Text(stringResource(Res.string.login_network_error))
                                }

                                ApiStatus.DEFAULT,
                                ApiStatus.LOADING
                                    -> {
                                    ContainedLoadingIndicator()
                                }

                                ApiStatus.SUCCESS -> {
                                    val qrContent = qrCodeInfoState.data?.url
                                        ?.let { it + QR_CODE_LOGIN_FINISH_DEEP_LINK }
                                    if (qrContent == null) {
                                        ASIconButton(onClick = { updateQrCode() }) {
                                            Icon(
                                                Icons.Outlined.Replay,
                                                contentDescription = stringResource(Res.string.common_retry)
                                            )
                                        }
                                    } else {
                                        // 本地生成二维码：此前是把内容交给
                                        // pan.misakamoe.com 出图，现改为纯本地编码，
                                        // 不再把登录地址（含 qrcode_key）发给第三方。
                                        val qrPainter = rememberQrCodePainter(
                                            data = qrContent,
                                            options = defaultQrOptions(),
                                        )
                                        Image(
                                            painter = qrPainter,
                                            contentDescription = stringResource(Res.string.cd_login_qrcode),
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    updateQrCode()
                                                })
                                    }
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = stringResource(Res.string.cd_not_agree_privacy)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(Res.string.login_check_privacy_policy))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                PlatformToggleButton(route, selectedLoginPlatform, updateLoginPlatform)
            }
            ASAgreePrivacyPolicy(agreePrivacyPolicy, onClick = {
                updateAgreePrivacyPolicy(!agreePrivacyPolicy)
            })
            Spacer(Modifier.height(15.dp))
            ASWarringTip{
                Text(
                    "登录后将自动切换对应的平台解析，也可在设置切换，如果您不熟悉，建议直接Web登录。",
                    fontSize = 14.sp
                )
            }
        }
    }
}
