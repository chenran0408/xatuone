package com.xingheyuzhuan.shiguangschedule.xatu.campus

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.RoundingMode

class XatuPaySessionExpired(hasToken: Boolean) : IllegalStateException(
    if (hasToken) "缴费平台登录已失效" else "请先登录缴费平台"
)
class XatuElectricityNoData : IllegalStateException("缴费平台暂未返回该房间的电量")

internal fun payResponseData(element: JsonElement, hasToken: Boolean): JsonElement {
    val value = element as? JsonObject ?: return element
    val code = value["messageCode"]?.jsonPrimitive?.content
    if (code in listOf("-1", "-2", "-3", "-4", "-5", "50014")) throw XatuPaySessionExpired(hasToken)
    check(code == null || code == "0") { value["message"]?.jsonPrimitive?.content ?: "缴费平台返回失败" }
    return value["data"] ?: element
}

/** querySydl returns kWh (one 度); suml - oddl is the remaining amount when suml is supplied. */
internal fun remainingElectricityInDegrees(row: JsonObject): String {
    val amount = row["oddl"]!!.jsonPrimitive.content.toBigDecimal()
    val total = row["suml"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toBigDecimal()
    return (total?.minus(amount) ?: amount).setScale(2, RoundingMode.HALF_UP).toPlainString()
}
