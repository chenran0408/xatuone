package com.xingheyuzhuan.shiguangschedule

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

class NavigationStateTest {
    @Test
    fun primaryTabSwitchKeepsOtherPrimaryEntries() {
        val stack = mutableListOf<NavKey>(Destination.CourseSchedule)
        stack.navigateTo(Destination.XatuHome)
        stack.navigateTo(Destination.Settings)
        stack.navigateTo(Destination.MoreOptions)
        stack.navigateTo(Destination.ThemeSettings)
        stack.navigateTo(Destination.CourseSchedule)

        assertEquals(
            listOf<NavKey>(Destination.XatuHome, Destination.Settings, Destination.CourseSchedule),
            stack
        )
    }

    @Test
    fun repeatedTabSwitchDoesNotDuplicateEntries() {
        val stack = mutableListOf<NavKey>(Destination.XatuHome)
        repeat(3) {
            stack.navigateTo(Destination.Settings)
            stack.navigateTo(Destination.XatuHome)
        }

        assertEquals(listOf<NavKey>(Destination.Settings, Destination.XatuHome), stack)
    }

    @Test
    fun aServiceOpenedFromHomeReturnsToHomeAfterItsWebPage() {
        val stack = mutableListOf<NavKey>(Destination.XatuHome)
        stack.navigateTo(Destination.XatuService("calendar"))
        stack.navigateTo(Destination.WebView(initialUrl = "https://jwcw.xatu.edu.cn/"))
        stack.removeAt(stack.lastIndex)
        assertEquals(Destination.XatuService("calendar"), stack.last())
        stack.removeAt(stack.lastIndex)
        assertEquals(Destination.XatuHome, stack.last())
    }

    @Test
    fun switchingTabsKeepsTheDirectoryAndDropsOnlyItsDetailRoutes() {
        val stack = mutableListOf<NavKey>(Destination.XatuHome)
        stack.navigateTo(Destination.XatuAcademic)
        stack.navigateTo(Destination.XatuService("grades"))
        stack.navigateTo(Destination.CourseSchedule)
        assertEquals(listOf<NavKey>(Destination.XatuHome, Destination.XatuAcademic, Destination.CourseSchedule), stack)
        assertFalse(Destination.TodaySchedule.isMainScreen)
    }

    @Test
    fun courseSettingsReturnThroughTheCourseHub() {
        val stack = mutableListOf<NavKey>(Destination.CourseSchedule)
        stack.navigateTo(Destination.CourseTools)
        stack.navigateTo(Destination.CourseSettings)
        stack.removeAt(stack.lastIndex)
        assertEquals(Destination.CourseTools, stack.last())
        stack.removeAt(stack.lastIndex)
        assertEquals(Destination.CourseSchedule, stack.last())
    }

    @Test
    fun academicAndCampusDirectoriesKeepTheirOwnReturnDestination() {
        listOf(Destination.XatuAcademic to "calendar", Destination.XatuServices to "electricity").forEach { (directory, page) ->
            val stack = mutableListOf<NavKey>(Destination.XatuHome)
            stack.navigateTo(directory)
            stack.navigateTo(Destination.XatuService(page))
            stack.removeAt(stack.lastIndex)
            assertEquals(directory, stack.last())
        }
    }

    @Test
    fun campusConfigurationReturnsToItsSettingsCategory() {
        val stack = mutableListOf<NavKey>(Destination.Settings)
        stack.navigateTo(Destination.CampusSettings)
        stack.navigateTo(Destination.XatuService("electricitySettings"))
        stack.removeAt(stack.lastIndex)
        assertEquals(Destination.CampusSettings, stack.last())
        stack.removeAt(stack.lastIndex)
        assertEquals(Destination.Settings, stack.last())
    }

    @Test
    fun newDestinationsAndTheExistingTodayRouteCanBeRestored() {
        val json = Json { serializersModule = navSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        listOf(
            Destination.AcademicSettings,
            Destination.CampusSettings,
            Destination.XatuService("electricitySettings"),
            Destination.XatuAcademic,
            Destination.XatuServices,
            Destination.XatuService("exams"),
            Destination.CourseTools,
            Destination.CourseSettings,
            Destination.CourseStartDateSettings,
            Destination.PrivacySettings,
            Destination.WebSettings,
            Destination.TodaySchedule,
        ).forEach { destination ->
            assertEquals(destination, json.decodeFromString(serializer, json.encodeToString(serializer, destination)))
        }
    }
}
