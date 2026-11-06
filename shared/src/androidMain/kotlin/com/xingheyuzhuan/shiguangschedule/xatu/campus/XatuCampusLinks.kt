package com.xingheyuzhuan.shiguangschedule.xatu.campus

object XatuCampusLinks {
    val pages = listOf(
        XatuCampusLink("charge", "电费充值", "https://jfpay.xatu.edu.cn/casLogin/"),
        XatuCampusLink("netfee", "校园网充值", "https://self.xatu.edu.cn/Self/sso/packageChange"),
        XatuCampusLink("checkin", "签到", "https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/*default/index.do#/jrqd"),
        XatuCampusLink("leave", "请销假", "https://ehall.xatu.edu.cn/xsfw/sys/swmxsqjapp/*default/index.do?wxType=1"),
        XatuCampusLink("library", "图书馆", "https://lib.xatu.edu.cn/"),
        XatuCampusLink("eams", "教务系统", "http://jwgl2018.xatu.edu.cn/"),
        XatuCampusLink("calendar", "校历", "https://jwcw.xatu.edu.cn/index/xagydxxl.htm"),
        XatuCampusLink("ehall", "一网通办", "https://ywtb.xatu.edu.cn/")
    )

    fun find(id: String): XatuCampusLink = pages.first { it.id == id }
}
