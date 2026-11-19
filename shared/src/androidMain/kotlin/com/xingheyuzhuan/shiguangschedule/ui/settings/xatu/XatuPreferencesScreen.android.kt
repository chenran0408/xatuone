package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import org.koin.compose.koinInject

@Composable
actual fun XatuPreferencesScreen(campus: Boolean, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val accounts: XatuAccountRepository = koinInject()
    val account by accounts.account.collectAsState(initial = null)
    val repository: XatuCampusRepository = koinInject()
    val snapshot by repository.snapshot.collectAsState(initial = null)
    XatuPage(if (campus) "校园服务设置" else "教务设置", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (campus) {
                item {
                    SectionHeading("服务配置")
                    UiCard(Modifier.fillMaxWidth()) {
                        UiServiceRow("电费与宿舍设置", snapshot?.dorm?.let { "${it.areaName.trim()} · ${it.buildingName.trim()} · ${it.roomName.trim()}" } ?: "未选择房间", { onNavigate(Destination.XatuService("electricitySettings")) })
                        UiServiceRow("签到位置与定位权限", "", { onNavigate(Destination.XatuService("checkinLocation")) })
                    }
                }
                item {
                    SectionHeading("服务")
                    UiCard(Modifier.fillMaxWidth()) {
                        UiServiceRow("校园服务", "", { onNavigate(Destination.XatuServices) })
                    }
                }
            } else {
                item {
                    SectionHeading("查询账号")
                    UiCard(Modifier.fillMaxWidth()) {
                        UiServiceRow("默认教务账号", account?.let { if (it.studentNumber.isBlank()) "尚未添加账号" else "${it.studentNumber} · ${if (it.loggedIn) "已连接" else "需要登录"}" } ?: "正在读取…", { onNavigate(Destination.XatuLogin) })
                    }
                }
                item {
                    SectionHeading("教务查询")
                    UiCard(Modifier.fillMaxWidth()) {
                        academicServices.filter { it.destination is Destination.XatuService }.forEach {
                            UiServiceRow(if (it.title == "成绩查询") "查看成绩与学分" else "查看${it.title}", "", { onNavigate(it.destination) })
                        }
                    }
                }
            }
        }
    }
}
