package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.koinInject
import school_index.AdapterCategory
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.calendar_today_24px
import shiguangschedule.shared.generated.resources.class_24px
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.home_24px
import shiguangschedule.shared.generated.resources.location_on_24px
import shiguangschedule.shared.generated.resources.person_24px
import shiguangschedule.shared.generated.resources.school_24px
import shiguangschedule.shared.generated.resources.search_24px
import shiguangschedule.shared.generated.resources.view_week_24px

private const val XATU_LOGIN_URL = "https://authserver.xatu.edu.cn/authserver/login?service=http://jwgl2018.xatu.edu.cn/eams/homeExt.action"

actual suspend fun clearXatuSession() = suspendCancellableCoroutine { continuation ->
    android.webkit.CookieManager.getInstance().removeAllCookies {
        android.webkit.CookieManager.getInstance().flush()
        continuation.resume(Unit)
    }
}

@Composable
actual fun XatuLoginScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    XatuWorkspaceScreen(onNavigate, onBack, showBack = true)
}

@Composable
actual fun XatuHomeScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    XatuWorkspaceScreen(onNavigate, onBack, showBack = false)
}

@Composable
private fun XatuWorkspaceScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit, showBack: Boolean) {
    val accountRepository: XatuAccountRepository = koinInject()
    val campusRepository: XatuCampusRepository = koinInject()
    val academicRepository: XatuAcademicRepository = koinInject()
    val savedAccount by accountRepository.account.collectAsState(initial = XatuAccount("", false))
    var account by rememberSaveable { mutableStateOf("") }
    var selectedPage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(savedAccount.studentNumber) {
        if (account.isBlank()) account = savedAccount.studentNumber
    }

    if (selectedPage != null) {
        Column(Modifier.fillMaxSize()) {
            PageHeader(pageTitle(selectedPage!!)) { selectedPage = null }
            XatuServicePage(
                selectedPage!!,
                { url -> onNavigate(Destination.WebView(initialUrl = url)) },
                Modifier.fillMaxSize().padding(horizontal = 16.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(vectorResource(Res.drawable.arrow_back_24px), stringResource(Res.string.a11y_back))
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("一校通", style = MaterialTheme.typography.headlineSmall)
                    Text("西安工业大学", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(vectorResource(Res.drawable.home_24px), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            AccountPanel(
                savedAccount = savedAccount,
                account = account,
                onAccountChange = { account = it },
                onLogin = {
                    scope.launch {
                        val next = account.trim()
                        if (savedAccount.studentNumber.isNotBlank() && savedAccount.studentNumber != next) {
                            clearXatuSession()
                            campusRepository.clear()
                            academicRepository.clearAccount(savedAccount.studentNumber)
                            accountRepository.markLoggedOut()
                        }
                        accountRepository.saveStudentNumber(next)
                        onNavigate(Destination.WebView(XATU_LOGIN_URL, xatuLogin = true))
                    }
                },
                onLogout = {
                    scope.launch {
                        clearXatuSession()
                        campusRepository.clear()
                        if (savedAccount.studentNumber.isNotBlank()) academicRepository.clearAccount(savedAccount.studentNumber)
                        accountRepository.clear()
                        account = ""
                    }
                }
            )
        }
        item { SectionHeading("学习", "课表、成绩与考试") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PortalAction("同步课表", "导入教务课表", Res.drawable.view_week_24px, Modifier.weight(1f)) {
                        onNavigate(Destination.AdapterSelection("XATU", "西安工业大学", AdapterCategory.BACHELOR_AND_ASSOCIATE.value, "XATU"))
                    }
                    PortalAction("成绩查询", "成绩与绩点", Res.drawable.search_24px, Modifier.weight(1f)) { selectedPage = "grades" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PortalAction("考试安排", "时间与考场", Res.drawable.calendar_today_24px, Modifier.weight(1f)) { selectedPage = "exams" }
                    PortalAction("学业进度", "培养方案与学分", Res.drawable.school_24px, Modifier.weight(1f)) { selectedPage = "plan" }
                }
            }
        }
        item { SectionHeading("校园生活", "常用校园服务") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PortalAction("电费余额", "宿舍用电记录", Res.drawable.class_24px, Modifier.weight(1f)) { selectedPage = "electricity" }
                    PortalAction("签到", "定位签到", Res.drawable.location_on_24px, Modifier.weight(1f)) { selectedPage = "checkinLocation" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PortalAction("校历", "学期日期与假期", Res.drawable.calendar_today_24px, Modifier.weight(1f)) { selectedPage = "calendar" }
                    PortalAction("一站式大厅", "办事与服务", Res.drawable.home_24px, Modifier.weight(1f)) {
                        onNavigate(Destination.WebView(initialUrl = "https://ehall.xatu.edu.cn/"))
                    }
                }
            }
        }
        item {
            SectionHeading("更多服务", "学校网站入口")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    ServiceLink("请销假", "申请与记录") { onNavigate(Destination.WebView(initialUrl = "https://ehall.xatu.edu.cn/xsfw/sys/swmxsqjapp/*default/index.do?wxType=1")) }
                    ServiceLink("图书馆", "馆藏与借阅") { onNavigate(Destination.WebView(initialUrl = "https://lib.xatu.edu.cn/")) }
                    ServiceLink("校园网", "网费充值与套餐管理") { onNavigate(Destination.WebView(initialUrl = "https://self.xatu.edu.cn/")) }
                    ServiceLink("电费充值", "打开缴费平台") { onNavigate(Destination.WebView(initialUrl = "https://jfpay.xatu.edu.cn/casLogin/")) }
                    ServiceLink("教务系统", "打开教务系统首页") { onNavigate(Destination.WebView(initialUrl = "http://jwgl2018.xatu.edu.cn/")) }
                    ServiceLink("一网通办", "学校综合办事入口") { onNavigate(Destination.WebView(initialUrl = "https://ywtb.xatu.edu.cn/")) }
                }
            }
        }
    }
}

@Composable
private fun AccountPanel(
    savedAccount: XatuAccount,
    account: String,
    onAccountChange: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    var editingAccount by rememberSaveable(savedAccount.studentNumber) {
        mutableStateOf(!savedAccount.loggedIn)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(vectorResource(Res.drawable.person_24px), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (savedAccount.loggedIn) "教务已连接" else "连接教务账号", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (savedAccount.loggedIn) savedAccount.studentNumber else "登录后可同步课表并查询学业信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (savedAccount.loggedIn && !editingAccount) {
                    TextButton(onClick = {
                        onAccountChange("")
                        editingAccount = true
                    }) { Text("切换") }
                } else {
                    Text(
                        if (savedAccount.loggedIn) "已登录" else "未登录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!savedAccount.loggedIn || editingAccount) {
                OutlinedTextField(
                    value = account,
                    onValueChange = onAccountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("学号") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onLogin, enabled = account.isNotBlank(), modifier = Modifier.weight(1f)) {
                        Text("网页登录")
                    }
                    if (savedAccount.loggedIn) {
                        TextButton(onClick = { editingAccount = false }) { Text("取消") }
                        TextButton(onClick = onLogout) { Text("退出登录") }
                    }
                }
            } else {
                TextButton(onClick = onLogout, modifier = Modifier.align(Alignment.End)) { Text("退出登录") }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(vectorResource(Res.drawable.arrow_back_24px), stringResource(Res.string.a11y_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PortalAction(title: String, subtitle: String, icon: DrawableResource, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(92.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(vectorResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ServiceLink(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(vectorResource(Res.drawable.chevron_right_24px), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

private fun pageTitle(page: String): String = when (page) {
    "grades" -> "成绩查询"
    "exams" -> "考试安排"
    "plan" -> "学业进度"
    "calendar" -> "校历"
    "electricity" -> "电费余额"
    "checkinLocation" -> "签到位置"
    else -> "校园服务"
}
