package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import androidx.compose.runtime.rememberCoroutineScope
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.arrow_back_24px
import school_index.AdapterCategory

private const val XATU_LOGIN_URL =
    "https://authserver.xatu.edu.cn/authserver/login?service=http://jwgl2018.xatu.edu.cn/eams/homeExt.action"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XatuLoginScreenShared(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
) {
    val accountRepository: XatuAccountRepository = koinInject()
    val campusRepository: XatuCampusRepository = koinInject()
    val savedAccount by accountRepository.account.collectAsState(initial = XatuAccount("", false))
    var account by rememberSaveable { mutableStateOf("") }
    var selectedPage by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(savedAccount.studentNumber) {
        if (account.isBlank()) account = savedAccount.studentNumber
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedPage?.let { pageTitle(it) } ?: "用户管理") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedPage != null) selectedPage = null else onBack() }) {
                        Icon(
                            vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (selectedPage != null) {
            XatuServicePage(
                page = selectedPage!!,
                onOpen = { url -> onNavigate(Destination.WebView(initialUrl = url)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("教务账号")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (savedAccount.studentNumber.isBlank() || !savedAccount.loggedIn) {
                        OutlinedTextField(
                            value = account,
                            onValueChange = { account = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("学号") },
                            singleLine = true
                        )
                    } else {
                        Text(savedAccount.studentNumber, style = MaterialTheme.typography.titleLarge)
                        Text("已登录", color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val next = account.trim()
                                if (savedAccount.studentNumber.isNotBlank() && savedAccount.studentNumber != next) {
                                    campusRepository.clear()
                                }
                                accountRepository.saveStudentNumber(next)
                            }
                            onNavigate(
                                Destination.WebView(
                                    initialUrl = XATU_LOGIN_URL,
                                    assetJsPath = null,
                                    xatuLogin = true
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = account.trim().isNotEmpty(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(if (savedAccount.loggedIn) "切换账号" else "使用网页登录")
                    }
                    Text("登录成功后返回此页，后续服务会复用当前会话。")
                    if (savedAccount.loggedIn) {
                        OutlinedButton(
                            onClick = {
                                clearXatuSession()
                                coroutineScope.launch {
                                    campusRepository.clear()
                                    accountRepository.clear()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("退出登录")
                        }
                    }
                }
            }

            Text("学年与学期")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("学期由教务系统返回")
                    Text("同步课表时默认使用当前学期，可在导入流程中切换。", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("数据同步")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onNavigate(
                                Destination.AdapterSelection(
                                    schoolId = "XATU",
                                    schoolName = "西安工业大学",
                                    categoryNumber = AdapterCategory.BACHELOR_AND_ASSOCIATE.value,
                                    resourceFolder = "XATU"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("同步课表")
                    }
                    ServiceButton("查询成绩", "查看当前或历史学期成绩") {
                        selectedPage = "grades"
                    }
                    ServiceButton("查询考试信息", "查看考试时间、考场和座位") {
                        selectedPage = "exams"
                    }
                    ServiceButton("查看校历", "查看假期和学期安排") {
                        selectedPage = "calendar"
                    }
                }
            }

            Text("校园服务")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceButton("校园服务", "打开西安工业大学一站式服务") {
                        onNavigate(Destination.WebView(initialUrl = "https://ehall.xatu.edu.cn/"))
                    }
                    ServiceButton("电费余额", "查看宿舍电费和充值入口") {
                        selectedPage = "electricity"
                    }
                    ServiceButton("网费服务", "打开校园网服务页面") {
                        onNavigate(Destination.WebView(initialUrl = "https://self.xatu.edu.cn/"))
                    }
                }
            }
        }
    }
}

@Composable
expect fun XatuLoginScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
)

/** Android 的 XATU 一级服务首页；其他平台可由对应 source set 提供占位实现。 */
@Composable
expect fun XatuHomeScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
)

private fun pageTitle(page: String): String = when (page) {
    "grades" -> "成绩查询"
    "exams" -> "考试安排"
    "calendar" -> "校历"
    "electricity" -> "电费余额"
    else -> "校园服务"
}

@Composable
expect fun XatuServicePage(
    page: String,
    onOpen: (String) -> Unit,
    modifier: Modifier
): Unit

expect fun clearXatuSession()

@Composable
private fun ServiceButton(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title)
            Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
