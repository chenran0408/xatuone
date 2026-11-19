package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import com.xingheyuzhuan.shiguangschedule.Destination
import school_index.AdapterCategory

internal val xatuCourseImport = Destination.AdapterSelection("XATU", "西安工业大学", AdapterCategory.BACHELOR_AND_ASSOCIATE.value, "XATU")

@Composable
internal expect fun XatuCourseSyncDialog(initialTableId: String?, onDismiss: () -> Unit, onLogin: (Destination) -> Unit)

@Composable
expect fun XatuLoginScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit, courseSyncTableId: String? = null)

@Composable
expect fun XatuHomeScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit, navigationPadding: PaddingValues)

@Composable
expect fun XatuServicesScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit)

@Composable
expect fun XatuAcademicScreen(onNavigate: (Destination) -> Unit)

@Composable
expect fun XatuSystemSettingsScreen(onBack: () -> Unit)

@Composable
expect fun XatuServiceScreen(page: String, onNavigate: (Destination) -> Unit, onBack: () -> Unit)

@Composable
expect fun XatuServicePage(page: String, onOpen: (String, String) -> Unit, modifier: Modifier, onLogin: () -> Unit, onCourseSettings: () -> Unit)

@Composable
expect fun XatuCourseSyncContext(onAccount: () -> Unit)

@Composable
expect fun XatuPreferencesScreen(campus: Boolean, onNavigate: (Destination) -> Unit, onBack: () -> Unit)
