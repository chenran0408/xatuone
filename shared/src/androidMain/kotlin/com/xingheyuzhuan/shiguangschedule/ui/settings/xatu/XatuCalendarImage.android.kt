package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.xingheyuzhuan.shiguangschedule.ui.components.UiActionButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiFeedback
import com.xingheyuzhuan.shiguangschedule.ui.components.UiFeedbackKind
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import java.io.File

@Composable
internal fun XatuCalendarImage(path: String, fetchedAt: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val request = remember(path, fetchedAt) {
        ImageRequest.Builder(context).data(File(path)).size(coil3.size.Size.ORIGINAL)
            .memoryCacheKey("$path:$fetchedAt").build()
    }
    var imageState by remember(request) { mutableStateOf(UiFeedbackKind.LOADING) }
    var scale by remember(path, fetchedAt) { mutableFloatStateOf(1f) }
    var offset by remember(path, fetchedAt) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        val maxX = viewport.width * (scale - 1) / 2
        val maxY = viewport.height * (scale - 1) / 2
        offset = Offset((offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY))
    }
    val colors = uiStyleColors()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UiText("校历原图")
            Box(
                Modifier.weight(1f).fillMaxWidth().clipToBounds().onSizeChanged { viewport = it }.transformable(transform),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = "校历原图",
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                    },
                    onLoading = { imageState = UiFeedbackKind.LOADING },
                    onSuccess = { imageState = UiFeedbackKind.SUCCESS },
                    onError = { imageState = UiFeedbackKind.ERROR },
                )
                when (imageState) {
                    UiFeedbackKind.LOADING -> UiFeedback("正在打开原图…", UiFeedbackKind.LOADING)
                    UiFeedbackKind.ERROR -> UiFeedback("无法打开校历原图", UiFeedbackKind.ERROR)
                    else -> Unit
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UiActionButton(onClick = { scale = (scale + 1).coerceAtMost(6f) }, enabled = imageState == UiFeedbackKind.SUCCESS && scale < 6f, modifier = Modifier.weight(1f), text = "放大")
                UiActionButton(onClick = { scale = 1f; offset = Offset.Zero }, enabled = imageState == UiFeedbackKind.SUCCESS && scale > 1f, modifier = Modifier.weight(1f), text = "还原")
            }
            UiActionButton(onClick = onClose, modifier = Modifier.fillMaxWidth(), text = "关闭原图")
        }
    }
}
