package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import com.xingheyuzhuan.shiguangschedule.ui.components.UiFloatingScheduleActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.archive_24px
import shiguangschedule.shared.generated.resources.close_24px

@Composable
fun FloatingCourseBar(
    floatingCourse: CourseWithWeeks?,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = floatingCourse != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier.zIndex(10f)
    ) {
        floatingCourse?.let { cw ->
            UiFloatingScheduleActions(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), onCancelClick) { cancel ->
                Icon(
                    imageVector = vectorResource(Res.drawable.archive_24px), contentDescription = null,
                    tint = uiStyleColors().accent, modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.widthIn(max = 180.dp)) {
                    UiText(text = cw.course.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = uiStyleColors().onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.IconButton(onClick = cancel, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.close_24px),
                        contentDescription = stringResource(Res.string.action_cancel),
                        tint = uiStyleColors().onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
