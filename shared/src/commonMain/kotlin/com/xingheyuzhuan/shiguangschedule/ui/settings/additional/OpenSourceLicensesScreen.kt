package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import com.xingheyuzhuan.shiguangschedule.ui.components.UiLoadingIndicator
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSettingRow
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.text_loading_failed
import shiguangschedule.shared.generated.resources.title_open_source_licenses

// 定义加载状态密封接口
private sealed interface ResourceState<out T> {
    data object Loading : ResourceState<Nothing>
    data class Success<T>(val data: T) : ResourceState<T>
    data class Error(val message: String) : ResourceState<Nothing>
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {

    val librariesState by produceState<ResourceState<Libs?>>(initialValue = ResourceState.Loading) {
        value = try {
            val jsonString = Res.readBytes("files/aboutlibraries.json").decodeToString()
            val libs = Libs.Builder().withJson(jsonString).build()
            ResourceState.Success(libs)
        } catch (e: Exception) {
            ResourceState.Error(e.message ?: e.toString())
        }
    }

    UiScaffold(
        containerColor = uiStyleColors().background,
        topBar = {
            UiTopAppBar(title = stringResource(Res.string.title_open_source_licenses),
                onBack = onBack)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = librariesState) {
                is ResourceState.Loading -> {
                    UiLoadingIndicator()
                }
                is ResourceState.Success -> {
                    if (LocalUiStyle.current == UiStyle.COUI) {
                        CouiLicenseList(state.data?.libraries.orEmpty())
                    } else LibrariesContainer(
                        libraries = state.data,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ResourceState.Error -> {
                    UiText(
                        text = stringResource(Res.string.text_loading_failed, state.message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiLicenseList(libraries: List<Library>) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(libraries, key = { it.uniqueId }) { library ->
            UiSettingRow(
                title = library.name,
                summary = listOfNotNull(library.artifactVersion, library.author.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
                onClick = { selectedId = library.uniqueId },
            )
        }
    }
    val library = selectedId?.let { id -> libraries.find { it.uniqueId == id } } ?: return
    UiDialog(
        title = library.name,
        onDismissRequest = { selectedId = null },
        dismissButton = UiDialogAction("关闭", { selectedId = null }),
        text = {
            UiText(library.uniqueId)
            library.description?.takeIf { it.isNotBlank() }?.let { UiText(it) }
            listOfNotNull(
                library.website?.let { "官方网站" to it },
                library.scm?.url?.let { "源代码" to it },
                library.funding.firstOrNull()?.url?.let { "赞助" to it },
            ).forEach { (label, url) ->
                UiSettingRow(title = label, onClick = { uriHandler.openUri(url) })
            }
            library.licenses.forEach { license ->
                val url = license.url
                if (url.isNullOrBlank()) UiText(license.name)
                else UiSettingRow(title = license.name, onClick = { uriHandler.openUri(url) })
                license.strippedLicenseContent?.let { UiText(it) }
            }
        },
    )
}
