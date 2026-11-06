package com.xingheyuzhuan.shiguangschedule.xatu.campus

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.RoundingMode
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.annotation.Single

@Single
class XatuCampusService(
    private val cookieManager: CookieManager,
    private val client: OkHttpClient
) {
    private val payBase = "https://jfpay.xatu.edu.cn/"

    private fun request(path: String, params: Map<String, String>? = null): JsonElement {
        val url = payBase + "api/" + path
        val builder = Request.Builder()
            .url(url)
            .header("Cookie", cookieManager.getCookie(url).orEmpty())
            .header("X-Token", cookieManager.getCookie(url).orEmpty().split("; ")
                .firstOrNull { it.startsWith("datalook_reimbursement_token=") }
                ?.substringAfter('=')
                .orEmpty())
            .header("Accept", "application/json")
        params?.let {
            val body = Json.encodeToString(it)
            builder.post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        }
        return client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) error("缴费平台返回 HTTP ${response.code}")
            val element = Json.parseToJsonElement(response.body.string())
            val objectValue = element as? kotlinx.serialization.json.JsonObject
            if (objectValue?.get("messageCode")?.jsonPrimitive?.content?.let { it != "0" } == true) {
                error(objectValue["message"]?.jsonPrimitive?.content ?: "缴费平台返回失败")
            }
            objectValue?.get("data") ?: element
        }
    }

    private fun rows(element: JsonElement): List<kotlinx.serialization.json.JsonObject> = when (element) {
        is JsonArray -> element.map { it.jsonObject }
        else -> listOf(element.jsonObject)
    }

    suspend fun projects(): List<XatuCampusOption> = withContext(Dispatchers.IO) {
        rows(request("pay/project/getAllProjectList"))
            .filter { it["proModelUrl"]?.jsonPrimitive?.content in listOf("electric", "electricNewCapec") }
            .map { XatuCampusOption(it["id"]!!.jsonPrimitive.content, it["projectName"]?.jsonPrimitive?.content.orEmpty()) }
    }

    suspend fun areas(projectId: String): List<XatuCampusOption> = options(
        "querySchoolList", mapOf("projectId" to projectId), "schoolid", "schoolname"
    )

    suspend fun buildings(projectId: String, areaId: String): List<XatuCampusOption> = options(
        "queryBuildingList", mapOf("projectId" to projectId, "areaid" to areaId), "buildid", "buildname"
    )

    suspend fun rooms(projectId: String, areaId: String, buildingId: String): List<XatuCampusOption> = options(
        "queryRoomList", mapOf("projectId" to projectId, "areaid" to areaId, "buildid" to buildingId), "roomid", "roomname"
    )

    private suspend fun options(
        path: String,
        params: Map<String, String>,
        idKey: String,
        nameKey: String
    ): List<XatuCampusOption> = withContext(Dispatchers.IO) {
        rows(request("pay/web/payEleCostController/$path", params))
            .map { XatuCampusOption(it[idKey]!!.jsonPrimitive.content, it[nameKey]!!.jsonPrimitive.content) }
    }

    suspend fun loadPage(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Cookie", cookieManager.getCookie(url).orEmpty())
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("校园服务返回 HTTP ${response.code}")
            response.body.string()
        }
    }

    suspend fun electricityBalance(dorm: XatuDorm): String = withContext(Dispatchers.IO) {
        val row = rows(request("pay/web/payEleCostController/querySydl", mapOf(
            "projectId" to dorm.project.id,
            "areaid" to dorm.area.id,
            "buildid" to dorm.building.id,
            "roomid" to dorm.room.id
        ))).first()
        val used = row["oddl"]!!.jsonPrimitive.content.toBigDecimal()
        val total = row["suml"]?.jsonPrimitive?.content?.toBigDecimalOrNull()
        (total?.minus(used) ?: used).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}
