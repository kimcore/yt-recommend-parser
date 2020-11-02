package com.shilu.recommender

import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import kotlinx.coroutines.future.await
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.commons.io.IOUtils
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

internal class Requester(ipBlocks: String?) {
    private val ipBlock = if (ipBlocks != null) Ipv6Block(ipBlocks) else null
    private val planner = if (ipBlock != null) NanoIpRoutePlanner(listOf(ipBlock), true) else null
    private val currentAddress: InetAddress?
        get() = ipBlock?.getAddressAtIndex(planner!!.currentAddress)
    private val requestConfig: RequestConfig
        get() {
            val config = RequestConfig.custom()
            if (currentAddress != null) config.setLocalAddress(currentAddress)
            return config.build()
        }

    suspend fun get(url: String, headers: Map<String, String> = mapOf()): String {
        val httpClient: CloseableHttpAsyncClient = HttpAsyncClients.createDefault()
        httpClient.start()
        val future = CompletableFuture<String>()
        val request = HttpGet(url)
        request.config = requestConfig
        headers.forEach { request.addHeader(it.key, it.value) }
        execute(request, httpClient, future)
        return future.await()
    }

    private fun execute(request: HttpUriRequest, httpClient: CloseableHttpAsyncClient, future: CompletableFuture<String>) {
        httpClient.execute(request, object : FutureCallback<HttpResponse> {
            override fun completed(result: HttpResponse) {
                future.complete(IOUtils.toString(result.entity.content, StandardCharsets.UTF_8))
                httpClient.close()
            }

            override fun cancelled() {
                future.cancel(false)
                httpClient.close()
            }

            override fun failed(ex: Exception) {
                future.completeExceptionally(ex)
                httpClient.close()
            }
        })
    }
}