package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuDormSelection
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.electricityReadingSource
import com.xingheyuzhuan.shiguangschedule.xatu.campus.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class XatuElectricityTest {
    private val dorm = XatuDormSelection("1", "电费", "a", "校区", "b", "楼栋", "101", "101")

    @Test
    fun onlyPaymentOriginCanFinishPaymentLogin() {
        assertTrue(isPaymentPage("https://jfpay.xatu.edu.cn/casLogin/"))
        assertFalse(isPaymentPage("https://authserver.xatu.edu.cn/authserver/login"))
        assertFalse(isPaymentPage("https://example.org/?next=https://jfpay.xatu.edu.cn/"))
        assertFalse(isPaymentPage("http://jfpay.xatu.edu.cn/"))
    }

    @Test
    fun successfulHttpEnvelopeWithNoUserIsStillSignedOut() {
        fun identity(body: String) = requireSignedInPayUser(payResponseData(Json.parseToJsonElement(body), true))
        assertFailsWith<XatuPaySessionExpired> { identity("""{"messageCode":"0","data":null}""") }
        assertFailsWith<XatuPaySessionExpired> { identity("""{"messageCode":"0","data":{"id":null}}""") }
        identity("""{"messageCode":"0","data":{"id":42}}""")
    }

    @Test
    fun expiredCookieWaitsForTheNewAcceptedSessionWithoutRetryingTheOldOne() = runBlocking {
        val cookies = listOf(null, "old", "old", null, "new").iterator()
        var checks = 0
        awaitAcceptedPayCookie(cookies::next) {
            if (++checks == 1) throw XatuPaySessionExpired(true)
        }
        assertEquals(2, checks)
        assertFalse(cookies.hasNext())
    }

    @Test
    fun connectionFailureIsVisibleAndClosingLoginStopsWaiting() = runBlocking {
        assertFailsWith<java.io.IOException> {
            awaitAcceptedPayCookie({ "cookie" }) { throw java.io.IOException("offline") }
        }
        var checks = 0
        val waiting = launch(start = CoroutineStart.UNDISPATCHED) { awaitAcceptedPayCookie({ null }) { checks++ } }
        waiting.cancelAndJoin()
        assertTrue(waiting.isCancelled)
        assertEquals(0, checks)
    }

    @Test
    fun remainingAmountIsConvertedOnceAndRetainsNegativeReadings() {
        fun amount(json: String) = remainingElectricityInDegrees(Json.parseToJsonElement(json).jsonObject)
        assertEquals("28.50", amount("""{"oddl":"71.5","suml":"100"}"""))
        assertEquals("28.50", amount("""{"oddl":"28.5"}"""))
        assertEquals("28.50", amount("""{"oddl":"28.5","suml":null}"""))
        assertEquals("-1.25", amount("""{"oddl":"101.25","suml":"100"}"""))
        assertEquals("0.01", amount("""{"oddl":"0.005"}"""))
        assertFailsWith<NumberFormatException> { amount("""{"oddl":"28.5","suml":"unavailable"}""" ) }
    }

    @Test
    fun platformSessionCodesAreNotTreatedAsEmptyOrSuccessfulData() {
        listOf("-1", "-2", "-3", "-4", "-5", "50014").forEach { code ->
            val payload = Json.parseToJsonElement("""{"messageCode":"$code","data":[]}""")
            val expired = assertFailsWith<XatuPaySessionExpired> { payResponseData(payload, hasToken = true) }
            assertEquals("缴费平台登录已失效", expired.message)
        }
        val firstLogin = assertFailsWith<XatuPaySessionExpired> {
            payResponseData(Json.parseToJsonElement("""{"messageCode":-1,"data":""}"""), hasToken = false)
        }
        assertEquals("请先登录缴费平台", firstLogin.message)
        val empty = Json.parseToJsonElement("[]")
        assertEquals(empty, payResponseData(Json.parseToJsonElement("""{"messageCode":"0","data":[]}"""), hasToken = false))
        val failure = assertFailsWith<IllegalStateException> {
            payResponseData(Json.parseToJsonElement("""{"messageCode":"99","message":"学校服务不可用"}"""), hasToken = true)
        }
        assertEquals("学校服务不可用", failure.message)
    }

    @Test
    fun cacheIsNeverShownAsTheLatestResultAfterFailureOrWhileRefreshing() {
        val snapshot = XatuCampusSnapshot(dorm = dorm, balance = "28.50", balanceAt = 1000L)
        val success = XatuElectricityQueryState(dorm = dorm, checkedAt = 1000L)
        assertEquals("更新于", electricityReadingSource(snapshot, success))
        assertContains(electricityReadingSource(snapshot, success.copy(loading = true)), "上次更新")
        assertContains(electricityReadingSource(snapshot, success.copy(error = "连接失败")), "上次更新")
        assertContains(electricityReadingSource(snapshot, success.copy(requiresLogin = true, error = "登录失效")), "上次更新")
        assertContains(electricityReadingSource(snapshot, XatuElectricityQueryState()), "上次更新")
        assertContains(electricityReadingSource(snapshot, success.copy(checkedAt = 900L)), "上次更新")
        assertContains(electricityReadingSource(snapshot.copy(dorm = dorm.copy(roomId = "102")), success), "上次更新")
    }
}
