package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import kotlinx.serialization.encodeToString

data class CourseImportProgress(
    val running: Boolean = false,
    val message: String = "",
    val selection: String? = null,
    val savedCount: Int? = null,
    val completed: Boolean = false,
    val error: String? = null,
) {
    internal fun scriptReturned(failure: String?): CourseImportProgress =
        if (!running || completed) this else copy(
            running = false,
            error = failure ?: error ?: "同步未完成。脚本最后返回：$message",
        )

    internal fun complete(): CourseImportProgress = when {
        error != null -> copy(running = false)
        savedCount == null -> copy(running = false, error = "同步未完成：未保存课程")
        else -> copy(running = false, completed = true, message = "同步成功，保存 $savedCount 条课程")
    }
}

/** 观察现有 XATU 脚本末尾的异步任务；不改动适配脚本或公共桥接接口。 */
internal fun trackedCourseImportScript(tableId: String, source: String): String = """
    (function() {
        window.currentTableId = ${bridgeJson.encodeToString(tableId)};
        Promise.resolve().then(function() {
            return (0, eval)(${bridgeJson.encodeToString(source)});
        }).then(function() {
            window._shiguangNativeBridge.postMessage(JSON.stringify({action:'importScriptSettled'}));
        }, function(error) {
            window._shiguangNativeBridge.postMessage(JSON.stringify({action:'importScriptSettled',payload:JSON.stringify({message:String(error.message || error)})}));
        });
    })();
""".trimIndent()
