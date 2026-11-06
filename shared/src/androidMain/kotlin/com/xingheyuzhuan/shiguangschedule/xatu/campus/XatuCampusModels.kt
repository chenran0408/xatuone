package com.xingheyuzhuan.shiguangschedule.xatu.campus

data class XatuCampusOption(val id: String, val name: String)

data class XatuDorm(
    val project: XatuCampusOption,
    val area: XatuCampusOption,
    val building: XatuCampusOption,
    val room: XatuCampusOption
) {
    val key: String get() = listOf(project.id, area.id, building.id, room.id).joinToString("|")
    val label: String get() = "${area.name} · ${building.name} ${room.name}"
}

data class XatuElectricity(val dorm: XatuDorm, val balance: String, val checkedAt: Long)
data class XatuDailyBalance(val day: String, val balance: String)

data class XatuCampusLink(val id: String, val title: String, val url: String)
