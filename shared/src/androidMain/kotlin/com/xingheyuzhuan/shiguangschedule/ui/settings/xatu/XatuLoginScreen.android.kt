package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.person_24px

@Composable
internal fun AccountPanel(
    savedAccount: XatuAccount,
    account: String,
    onAccountChange: (String) -> Unit,
    actionInProgress: Boolean,
    status: String,
    onLogin: () -> Unit
) {
    var couiEdit by rememberSaveable { mutableStateOf(false) }
    var couiDraft by rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(account))
    }
    if (savedAccount.studentNumber.isBlank() && couiEdit) com.itos.coui.dialog.InputDialog(
        value = couiDraft, onValueChange = { couiDraft = it }, title = "学号", clearContentDescription = "清除学号",
        onDismissRequest = { couiEdit = false },
        confirmAction = com.itos.coui.dialog.CouiDialogAction("确定", { onAccountChange(couiDraft.text); couiEdit = false }),
        dismissAction = com.itos.coui.dialog.CouiDialogAction("取消", { couiEdit = false }))
    UiCard(modifier = Modifier.fillMaxWidth()) {
        val coui = LocalUiStyle.current == UiStyle.COUI
        Column(Modifier.fillMaxWidth().padding(if (coui) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (coui) 0.dp else 10.dp)) {
            if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
                ListItem(
                    overlineContent = { Text("教务账号") },
                    headlineContent = { Text(savedAccount.studentNumber.ifBlank { "未设置" }, softWrap = true) },
                    supportingContent = { Text(if (savedAccount.loggedIn) "已登录" else "未登录") },
                    leadingContent = { Icon(vectorResource(Res.drawable.person_24px), null) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                top.yukonga.miuix.kmp.basic.BasicComponent(
                    startAction = {
                        Box(Modifier.size(44.dp).background(MiuixTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center) {
                            top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.person_24px), null, tint = MiuixTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    insideMargin = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    top.yukonga.miuix.kmp.basic.Text("教务账号", style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    top.yukonga.miuix.kmp.basic.Text(savedAccount.studentNumber.ifBlank { "未设置" },
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title2)
                    top.yukonga.miuix.kmp.basic.Text(if (savedAccount.loggedIn) "已登录" else "未登录",
                        style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            } else com.itos.coui.layout.Item(
                icon = { Icon(vectorResource(Res.drawable.person_24px), null) },
                title = {
                    com.itos.coui.text.Text(
                        text = "教务账号",
                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                    )
                    com.itos.coui.text.Text(
                        text = savedAccount.studentNumber.ifBlank { "未设置" },
                        kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                    )
                },
                summary = {
                    com.itos.coui.text.Text(
                        text = if (savedAccount.loggedIn) "已登录" else "未登录",
                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                    )
                },
            )
            if (savedAccount.studentNumber.isBlank()) {
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                    com.itos.coui.layout.Item(onClick = {
                        couiDraft = androidx.compose.ui.text.input.TextFieldValue(account)
                        couiEdit = true
                    }, enabled = !actionInProgress, title = { UiText("学号") }, summary = { UiText(account.ifBlank { "未设置" }) })
                } else UiInputField(
                    value = account,
                    onValueChange = onAccountChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionInProgress,
                    label = "学号",
                    singleLine = true
                )
                androidx.compose.foundation.layout.FlowRow(
                    modifier = if (coui) Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp) else Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
                        Button(onClick = onLogin, enabled = account.isNotBlank() && !actionInProgress) {
                            if (actionInProgress) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(if (actionInProgress) "正在准备…" else "网页登录")
                        }
                    } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                        top.yukonga.miuix.kmp.basic.TextButton(if (actionInProgress) "正在准备…" else "网页登录", onLogin,
                            enabled = account.isNotBlank() && !actionInProgress,
                            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary())
                    } else UiActionButton(onClick = onLogin, enabled = account.isNotBlank() && !actionInProgress, text = if (actionInProgress) "正在准备…" else "网页登录")
                }
            } else {
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
                    Button(onClick = onLogin, enabled = !actionInProgress) {
                        if (actionInProgress) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(if (actionInProgress) "正在准备…" else "重新登录")
                    }
                } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                    top.yukonga.miuix.kmp.basic.TextButton(if (actionInProgress) "正在准备…" else "重新登录", onLogin,
                        enabled = !actionInProgress,
                        colors = if (savedAccount.loggedIn) top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors()
                            else top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary())
                } else {
                    com.itos.coui.layout.Item(
                        onClick = onLogin,
                        enabled = !actionInProgress,
                        title = { UiText("重新登录") },
                        summary = if (actionInProgress) ({ UiText("正在准备…") }) else null,
                        trailing = {
                            if (actionInProgress) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(vectorResource(Res.drawable.chevron_right_24px), null,
                                    tint = uiStyleColors().secondaryText)
                            }
                        },
                    )
                }
            }
            if (status.isNotBlank()) {
                UiText(
                    status,
                    modifier = if (coui) Modifier.padding(horizontal = 16.dp, vertical = 8.dp) else Modifier,
                    style = MaterialTheme.typography.bodySmall,
                    color = if ("失败" in status) MaterialTheme.colorScheme.error else uiStyleColors().secondaryText
                )
            }
        }
    }
}

@Composable
internal fun SectionHeading(title: String, subtitle: String = "") {
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
        top.yukonga.miuix.kmp.basic.SmallTitle(title, insideMargin = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp))
        if (subtitle.isNotBlank()) UiText(subtitle, style = MaterialTheme.typography.bodySmall)
        return
    }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        com.itos.coui.text.Text(text = title, kind = com.itos.coui.text.CouiTextKind.GroupTitle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Start)
        if (subtitle.isNotBlank()) UiText(subtitle, Modifier.padding(horizontal = 32.dp))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        UiText(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle.isNotBlank()) UiText(subtitle, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
    }
}
