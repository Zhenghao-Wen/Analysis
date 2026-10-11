package com.imcys.bilibilias.shared.feature.tools.donate

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import bilibilias.shared.generated.resources.*
import com.imcys.bilibilias.ui.component.ASAsyncImage
import com.imcys.bilibilias.ui.component.ASTopAppBar
import com.imcys.bilibilias.ui.component.AsBackIconButton
import com.imcys.bilibilias.ui.component.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.component.shimmer.shimmer
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object DonateRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(donateRoute: DonateRoute, onToBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    DonateScaffold(
        scrollBehavior = scrollBehavior,
        onToBack = onToBack
    ) {
        DonateContent(paddingValues = it)
    }
}

@Composable
fun DonateContent(paddingValues: PaddingValues) {

    var showPayDialog by remember { mutableStateOf(false) }
    var payUrl by remember { mutableStateOf("") }
    val vm = koinViewModel<DonateViewModel>()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    // 后端已停用：数据不会再到达，用 loadFailed 结束骨架态，避免永久 shimmer
    // 以及进度条算出 0/0。
    val dataUnavailable = uiState.loadFailed || uiState.oldDonateInfo == null

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (uiState.loadFailed) {
            Text(
                text = "捐助信息暂不可用（相关后端服务已停用）。",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Row {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shimmer(uiState.oldDonateInfo?.alipay == null && !uiState.loadFailed),
                shape = CardDefaults.shape,
                onClick = {
                    showPayDialog = true
                    payUrl = uiState.oldDonateInfo?.alipay ?: ""
                },
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_xiao_he_bao_24px),
                        contentDescription = null,
                        Modifier
                            .size(30.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(stringResource(Res.string.donate_recommend))
                            }
                        }
                    ) {
                        Text(
                            "支付宝小荷包",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shimmer(uiState.oldDonateInfo?.weChat == null && !uiState.loadFailed),
                shape = CardDefaults.shape,
                onClick = {
                    showPayDialog = true
                    payUrl = uiState.oldDonateInfo?.weChat ?: ""
                },
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_weixin_pay_24px),
                        contentDescription = null,
                        Modifier
                            .size(30.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(Res.string.donate_wechat), fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.shimmer(
                uiState.oldDonateInfo == null && !uiState.loadFailed
            ).fillMaxWidth(),
            shape = CardDefaults.shape
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    "本月进展（${uiState.oldDonateInfo?.surplus}/${uiState.oldDonateInfo?.total}）:",
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    modifier = Modifier.height(10.dp).fillMaxWidth(),
                    progress = {
                        // 数据不可用时不再计算，避免 0/0 = NaN
                        if (dataUnavailable) {
                            0f
                        } else {
                            val total = (uiState.oldDonateInfo?.total ?: "0.0").toDoubleOrNull() ?: 0.0
                            val surplus = (uiState.oldDonateInfo?.surplus ?: "0.0").toDoubleOrNull() ?: 0.0
                            if (total <= 0.0) 0f else (surplus / total).toFloat()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = """
                        BILIBILIAS自运营之初，就有后端支持，我们目前采用的是学生机，但即使如此，每月也会产生费用。
                        因此，希望BILIBILIAS的用户们可以支援一些费用，一次早餐钱，一次奶茶钱，一次午饭钱就可以满足BILIBILIAS绝大部分的费用。
                        最后，感谢所有为BILIBILIAS捐款和支持AS的用户。
                        
                        大家可以通过扫码支付宝小荷包查看捐款的资金总数和资金流动原因，公开透明。
                    """.trimIndent(),
                )
            }
        }

        PayBottomDialog(showPayDialog, payUrl, onDismissRequest = {
            showPayDialog = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayBottomDialog(showPayDialog: Boolean, payUrl: String, onDismissRequest: () -> Unit) {
    if (showPayDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(10.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                ASAsyncImage(
                    payUrl, contentDescription = "捐款二维码", shape = CardDefaults.shape,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    contentScale = ContentScale.FillWidth,
                )

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScaffold(
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
                    Text(text = "捐助我们")
                },
                navigationIcon = {
                    AsBackIconButton(onClick = {
                        onToBack.invoke()
                    })
                }
            )
        },
    ) {
        content(it)
    }

}
