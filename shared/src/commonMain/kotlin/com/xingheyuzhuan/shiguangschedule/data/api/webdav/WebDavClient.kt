package com.xingheyuzhuan.shiguangschedule.data.api.webdav

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException

/**
 * WebDAV 协议通信客户端
 * 职责：处理物理文件与目录的传输，支持多级目录级联创建。
 */
class WebDavClient(
    private val config: WebDavConfig
) : AutoCloseable {

    private val client by lazy {
        HttpClient {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = config.username, password = config.password)
                    }
                    sendWithoutRequest { true }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
            }
        }
    }

    private val normalizedBaseUrl: String
        get() = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"

    private fun buildFullUrl(relativePath: String): String {
        val root = config.getCleanRootPath()
        val relative = relativePath.trim('/')
        return "$normalizedBaseUrl$root$relative"
    }

    suspend fun ensureRootDirectoryExists(): Boolean {
        val rootDir = config.getCleanRootPath().trim('/')
        return ensureRemoteDirChainExists(rootDir)
    }

    private suspend fun ensureRemoteDirChainExists(relativeDirChain: String): Boolean {
        if (relativeDirChain.isEmpty()) return true

        val pathSegments = relativeDirChain.split('/').filter { it.isNotEmpty() }
        var currentPath = ""

        try {
            for (segment in pathSegments) {
                currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
                val fullUrl = "$normalizedBaseUrl$currentPath/"

                val response = client.request(fullUrl) {
                    method = HttpMethod("MKCOL")
                }

                val isSuccess = response.status == HttpStatusCode.Created ||
                        response.status == HttpStatusCode.MethodNotAllowed

                if (!isSuccess) return false
            }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 将备份模块直接上传到其远端路径。
     */
    suspend fun upload(remoteFileName: String, bytes: ByteArray): Boolean {
        val rootPrefix = config.getCleanRootPath()
        val fileRelativeDir = remoteFileName.substringBeforeLast('/', "")

        val fullRelativeDirChain = if (fileRelativeDir.isEmpty()) {
            rootPrefix.trim('/')
        } else {
            "${rootPrefix.trim('/')}/${fileRelativeDir.trim('/')}"
        }

        if (!ensureRemoteDirChainExists(fullRelativeDirChain)) return false

        return client.put(buildFullUrl(remoteFileName)) { setBody(bytes) }.status.isSuccess()
    }

    /**
     * 读取一个远端备份模块。
     */
    suspend fun download(remoteFileName: String): ByteArray? {
        val response = client.get(buildFullUrl(remoteFileName))
        return if (response.status.isSuccess()) response.body<ByteArray>() else null
    }

    override fun close() = client.close()
}
