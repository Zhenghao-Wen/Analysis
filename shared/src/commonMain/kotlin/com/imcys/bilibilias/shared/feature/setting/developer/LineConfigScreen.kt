package com.imcys.bilibilias.shared.feature.setting.developer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import bilibilias.shared.generated.resources.*
import com.imcys.bilibilias.ui.utils.ASSizeClass
import com.imcys.bilibilias.ui.utils.rememberWidthSizeClass
import com.imcys.bilibilias.ui.component.ASIconButton
import com.imcys.bilibilias.ui.component.ASTextButton
import com.imcys.bilibilias.ui.component.ASTopAppBar
import com.imcys.bilibilias.ui.component.AsBackIconButton
import com.imcys.bilibilias.ui.component.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.component.TipSettingsItem
import com.imcys.bilibilias.ui.component.shimmer.shimmer
import com.imcys.bilibilias.ui.component.tip.ASInfoTip
import com.imcys.bilibilias.ui.component.tip.ASWarringTip
import com.imcys.bilibilias.ui.component.maybeNestedScroll
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object LineConfigRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineConfigScreen(lineConfigRoute: LineConfigRoute, onToBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LineConfigScaffold(scrollBehavior = scrollBehavior, onToBack = onToBack) { paddingValues ->
        LineConfigContent(
            modifier = Modifier
                .padding(paddingValues)
                .maybeNestedScroll(scrollBehavior)
        )
    }
}

@Composable
fun LineConfigContent(modifier: Modifier) {
    val vm = koinViewModel<LineConfigViewModel>()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val biliLineHostListState by vm.biliLineHostListState.collectAsStateWithLifecycle()
    val widthSizeClass = rememberWidthSizeClass()

    LaunchedEffect(Unit) {
        vm.loadLineConfig()
    }

    val columns = when (widthSizeClass) {
        ASSizeClass.COMPACT -> 1
        ASSizeClass.MEDIUM, ASSizeClass.EXPANDED -> 2
    }
    LazyVerticalGrid(
        modifier = modifier
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        columns = GridCells.Fixed(columns)
    ) {

        item(
            span = { GridItemSpan(columns) }
        ) {
            ASInfoTip {
                Text(
                    """
                线路配置可在部分情况下重定向缓存资源的地址路径，从而加快缓存，但并不是任何时候都有用。
            """.trimIndent()
                )
            }
        }


        item(span = { GridItemSpan(columns) }) {
            AnimatedVisibility(visible = uiState.currentLineHost.isNotEmpty()) {
                ASWarringTip(
                    modifier = Modifier.animateItem()
                ) {
                    Text(
                        """
                            如果你发现使用非自动线路后出现视频无法播放等问题，请切换回自动线路，这可能是内置的线路已经被废弃。
                        """.trimIndent()
                    )
                }
            }
        }

        item(span = { GridItemSpan(columns) }) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                ASIconButton(onClick = {
                    vm.startSpeedTest()
                }) {
                    Icon(Icons.Outlined.Speed, contentDescription = "图标")
                }
            }
        }

        biliLineHostListState.forEach {
            item {
                LineHostCard(
                    uiState = uiState,
                    item = it,
                    onUpdateLineHost = vm::updateLineHost,
                    onStartSpeedTest = vm::startSpeedTest
                )
            }
        }

        item {
            TipSettingsItem(
                modifier =  Modifier.animateItem(),
                text =
                """
                    这些线路由 B 站支持的 CDN 提供，不经过本项目的任何服务。
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun LineHostCard(
    uiState: LineConfigUIState,
    item: BILILineHostItem,
    onUpdateLineHost: (String) -> Unit,
    onStartSpeedTest: (BILILineHostItem) -> Unit
) {
    val colorState by animateColorAsState(
        targetValue = if (uiState.currentLineHost == item.host)
            MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    )
    Surface(
        shape = CardDefaults.shape,
        // 选中颜色
        color = colorState,
        onClick = {
            onUpdateLineHost(item.host)
        }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.name, modifier = Modifier.weight(1f))

            ASTextButton(
                enabled = !item.checkSpeeding,
                modifier = Modifier
                    .shimmer(item.checkSpeeding)
                    .then(if (item.host.isNotEmpty()) Modifier else Modifier.alpha(0f)),
                onClick = {
                    onStartSpeedTest(item)
                }) {
                if (item.speed != null) {
                    Text("${item.speed}")
                } else {
                    Text("检测")
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineConfigScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    onToBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ASTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior,
                style = BILIBILIASTopAppBarStyle.Large,
                title = {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(stringResource(Res.string.common_beta))
                            }
                        }
                    ) {
                        Text(text = "线路配置")
                    }
                },
                navigationIcon = {
                    AsBackIconButton(onClick = {
                        onToBack.invoke()
                    })
                },
                alwaysDisplay = false
            )
        },
    ) {
        content(it)
    }

}
