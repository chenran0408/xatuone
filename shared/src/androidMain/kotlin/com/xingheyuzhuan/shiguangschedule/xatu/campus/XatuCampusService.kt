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
    private val payClient = client.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private val payBase = "https://jfpay.xatu.edu.cn/"

    private fun request(path: String, params: Map<String, String>? = null): JsonElement {
        val url = payBase + "api/" + path
        val token = cookieManager.getCookie(url).orEmpty().split("; ")
            .firstOrNull { it.startsWith("datalook_reimbursement_token=") }
            ?.substringAfter('=')
            .orEmpty()
        val hasToken = token.isNotBlank()
        val builder = Request.Builder()
            .url(url)
            .header("X-Token", token)
            .header("Accept", "application/json")
        params?.let {
            val body = Json.encodeToString(it)
            builder.post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        }
        return payClient.newCall(builder.build()).execute().use { response ->
            if (response.code == 401 || response.isRedirect) throw XatuPaySessionExpired(hasToken)
            if (response.code == 403) error("缴费平台拒绝访问（HTTP 403）")
            if (!response.isSuccessful) error("缴费平台返回 HTTP ${response.code}")
            payResponseData(Json.parseToJsonElement(response.body.string()), hasToken)
        }
    }

    private fun rows(element: JsonElement): List<kotlinx.serialization.json.JsonObject> = when (element) {
        is JsonArray -> element.map { it.jsonObject }
        else -> listOf(element.jsonObject)
    }

    suspend fun projects(): List<XatuCampusOption> = withContext(Dispatchers.IO) {
        rows(request("pay/project/getAllProjectList"))
            .filter { it["proModelUrl"]?.jsonPrimitive?.content in listOf("electric", "electricNewCapec") }
            .map { XatuCampusOption(it["id"]!!.jsonPrimitive.content, it["projectName"]?.jsonPrimitive?.content.orEmpty().trim()) }
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
            .map { XatuCampusOption(it[idKey]!!.jsonPrimitive.content, it[nameKey]!!.jsonPrimitive.content.trim()) }
    }

    suspend fun electricityBalance(dorm: XatuDorm): String = withContext(Dispatchers.IO) {
        val row = rows(request("pay/web/payEleCostController/querySydl", mapOf(
            "projectId" to dorm.project.id,
            "areaid" to dorm.area.id,
            "buildid" to dorm.building.id,
            "roomid" to dorm.room.id
        ))).firstOrNull() ?: throw XatuElectricityNoData()
        remainingElectricityInDegrees(row)
    }
}
