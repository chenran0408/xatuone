package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import school_index.AdapterCategory
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.arrow_back_24px
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private const val XATU_LOGIN_URL = "https://authserver.xatu.edu.cn/authserver/login?service=http://jwgl2018.xatu.edu.cn/eams/homeExt.action"

actual fun clearXatuSession() {
    android.webkit.CookieManager.getInstance().removeAllCookies(null)
    android.webkit.CookieManager.getInstance().flush()
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
    LaunchedEffect(savedAccount.studentNumber) { if (account.isBlank()) account = savedAccount.studentNumber }

    MiuixTheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedPage != null) {
                XatuPageHeader(pageTitle(selectedPage!!)) { selectedPage = null }
                XatuServicePage(selectedPage!!, { url -> onNavigate(Destination.WebView(initialUrl = url)) }, Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp))
            } else {
                if (showBack) XatuPageHeader("XATU One", onBack)
                XatuHomeContent(
                    savedAccount = savedAccount,
                    account = account,
                    onAccountChange = { account = it },
                    onLogin = {
                        scope.launch {
                            val next = account.trim()
                            if (savedAccount.studentNumber.isNotBlank() && savedAccount.studentNumber != next) {
                                campusRepository.clear()
                                academicRepository.clearAccount(savedAccount.studentNumber)
                            }
                            accountRepository.saveStudentNumber(next)
                        }
                        onNavigate(Destination.WebView(XATU_LOGIN_URL, xatuLogin = true))
                    },
                    onLogout = {
                        clearXatuSession()
                        scope.launch {
                            campusRepository.clear()
                            if (savedAccount.studentNumber.isNotBlank()) academicRepository.clearAccount(savedAccount.studentNumber)
                            accountRepository.clear()
                        }
                    },
                    onNavigate = onNavigate,
                    onService = { selectedPage = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun XatuPageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(vectorResource(Res.drawable.arrow_back_24px), stringResource(Res.string.a11y_back)) }
        MiuixText(title, fontSize = 20.sp)
    }
}

@Composable
private fun XatuHomeContent(
    savedAccount: XatuAccount,
    account: String,
    onAccountChange: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (Destination) -> Unit,
    onService: (String) -> Unit,
    modifier: Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Color(0xFF2563EB), CircleShape), contentAlignment = Alignment.Center) { MiuixText("一", color = Color.White, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                MiuixText("一校通", fontSize = 25.sp)
                MiuixText("西安工业大学校园服务", color = Color(0xFF6C7280), fontSize = 13.sp)
            }
            MiuixText(if (savedAccount.loggedIn) "已登录" else "未登录", color = if (savedAccount.loggedIn) Color(0xFF16834C) else Color(0xFF9A6700), fontSize = 12.sp)
        }
        MiuixCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        MiuixText(if (savedAccount.loggedIn && savedAccount.studentNumber.isNotBlank()) "教务已连接" else "连接教务账号", fontSize = 18.sp)
                        MiuixText(if (savedAccount.loggedIn) savedAccount.studentNumber else "登录后同步课表、成绩和考试", color = Color(0xFF6C7280), fontSize = 13.sp)
                    }
                    Box(Modifier.size(10.dp).background(if (savedAccount.loggedIn) Color(0xFF2C9B68) else Color(0xFFE2A12A), CircleShape))
                }
                if (!savedAccount.loggedIn || savedAccount.studentNumber.isBlank()) {
                    OutlinedTextField(account, onAccountChange, Modifier.fillMaxWidth(), label = { Text("学号") }, singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MiuixButton(onClick = onLogin, enabled = account.trim().isNotEmpty(), modifier = Modifier.weight(1f), insideMargin = PaddingValues(vertical = 11.dp)) { MiuixText(if (savedAccount.loggedIn) "切换账号" else "网页登录") }
                    if (savedAccount.loggedIn) MiuixButton(onClick = onLogout, modifier = Modifier.weight(1f), insideMargin = PaddingValues(vertical = 11.dp)) { MiuixText("退出登录") }
                }
            }
        }
        SmallTitle("常用功能")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PortalAction("同步课表", "更新本周安排", Modifier.weight(1f), Color(0xFF2563EB)) { onNavigate(Destination.AdapterSelection("XATU", "西安工业大学", AdapterCategory.BACHELOR_AND_ASSOCIATE.value, "XATU")) }
            PortalAction("考试安排", "时间与考场", Modifier.weight(1f), Color(0xFF7C3AED)) { onService("exams") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PortalAction("成绩查询", "成绩与绩点", Modifier.weight(1f), Color(0xFF0F766E)) { onService("grades") }
            PortalAction("电费余额", "宿舍用电记录", Modifier.weight(1f), Color(0xFFD97706)) { onService("electricity") }
        }
        MiuixButton(onClick = { onService("calendar") }, modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { MiuixText("校历与学业进度", fontSize = 16.sp); MiuixText("查看学期日期、培养方案和毕业学分", color = Color(0xFF6C7280), fontSize = 12.sp) }; MiuixText("›", color = Color(0xFF8A8F9A), fontSize = 24.sp) }
        }
        SmallTitle("校园服务")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PortalAction("一站式大厅", "办事与服务", Modifier.weight(1f), Color(0xFF2563EB)) { onNavigate(Destination.WebView(initialUrl = "https://ehall.xatu.edu.cn/")) }
            PortalAction("签到", "定位签到", Modifier.weight(1f), Color(0xFF0891B2)) { onService("checkinLocation") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PortalAction("请销假", "申请与记录", Modifier.weight(1f), Color(0xFF4F46E5)) { onNavigate(Destination.WebView(initialUrl = "https://ehall.xatu.edu.cn/xsfw/sys/swmxsqjapp/*default/index.do?wxType=1")) }
            PortalAction("图书馆", "馆藏与借阅", Modifier.weight(1f), Color(0xFF15803D)) { onNavigate(Destination.WebView(initialUrl = "https://lib.xatu.edu.cn/")) }
        }
        SmallTitle("更多入口")
        MiuixCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ServiceAction("电费充值", "打开缴费平台", Color(0xFFD97706)) { onNavigate(Destination.WebView(initialUrl = "https://jfpay.xatu.edu.cn/casLogin/")) }
                ServiceAction("校园网", "网费充值与套餐管理", Color(0xFF7C3AED)) { onNavigate(Destination.WebView(initialUrl = "https://self.xatu.edu.cn/")) }
                ServiceAction("教务系统", "打开教务系统首页", Color(0xFF2563EB)) { onNavigate(Destination.WebView(initialUrl = "http://jwgl2018.xatu.edu.cn/")) }
                ServiceAction("一网通办", "学校综合办事入口", Color(0xFF0F766E)) { onNavigate(Destination.WebView(initialUrl = "https://ywtb.xatu.edu.cn/")) }
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun PortalAction(title: String, subtitle: String, modifier: Modifier, accent: Color, onClick: () -> Unit) {
    MiuixButton(onClick = onClick, modifier = modifier.height(92.dp), insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            MiuixText(title, fontSize = 16.sp)
            MiuixText(subtitle, color = Color(0xFF6C7280), fontSize = 12.sp)
        }
    }
}

@Composable
private fun XatuActionCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    MiuixButton(onClick = onClick, modifier = modifier.height(88.dp), insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) { MiuixText(title, fontSize = 16.sp); MiuixText(subtitle, color = Color(0xFF6C7280), fontSize = 12.sp) }
    }
}

@Composable
private fun ServiceAction(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    MiuixButton(onClick = onClick, Modifier.fillMaxWidth(), insideMargin = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(accent, CircleShape)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { MiuixText(title, fontSize = 16.sp); MiuixText(subtitle, color = Color(0xFF6C7280), fontSize = 12.sp) }
            MiuixText("›", color = Color(0xFF8A8F9A), fontSize = 24.sp)
        }
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

