package com.shilu.recommender

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shilu.recommender.entities.MalformedBodyException
import com.shilu.recommender.entities.MalformedJSONException
import com.shilu.recommender.entities.Recommendation
import com.shilu.recommender.entities.TimeoutException
import kotlinx.coroutines.withTimeoutOrNull

class RecommendAPI(ipBlocks: String?) {
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko"
    private val dataRegex = Regex("(window\\[\"ytInitialData\"]|var ytInitialData)\\s*=\\s*(.*);")
    private val requester: Requester = Requester(ipBlocks)

    suspend fun retrieveRecommends(url: String, timeout: Long = 5000L): List<Recommendation> {
        val body = withTimeoutOrNull(timeout) {
            requester.get(url, mapOf("User-Agent" to userAgent))
        } ?: throw TimeoutException("Could not retrieve body from YouTube within $timeout ms")

        val matchedData =
            dataRegex.find(body) ?: throw MalformedBodyException("Cannot match regex from the retrieved body")

        val json = try {
            Gson().fromJson(matchedData.groupValues[2], JsonObject::class.java)
        } catch (exception: Exception) {
            try {
                Gson().fromJson(matchedData.groupValues[2].split(";</script><script nonce=")[0], JsonObject::class.java)
            } catch (exception: Exception) {
                println(matchedData.groupValues[2])
                throw MalformedJSONException("Cannot parse JSON from the matched regex")
            }
        }

        val results = json["playerOverlays"]
            .asJsonObject["playerOverlayRenderer"]
            .asJsonObject["endScreen"]
            .asJsonObject["watchNextEndScreenRenderer"]
            .asJsonObject["results"]
            .asJsonArray.toList()
            .filter {
                try {
                    it.asJsonObject["endScreenVideoRenderer"].asJsonObject
                    true
                } catch (exception: Exception) {
                    false
                }
            }

        return results.map {
            val data = it.asJsonObject["endScreenVideoRenderer"].asJsonObject
            Recommendation(
                title = data["title"].asJsonObject["simpleText"].asString,
                identifier = data["videoId"].asString,
                length = data["lengthText"].asJsonObject["simpleText"].asString,
                lengthMs = data["lengthInSeconds"].asLong * 1000L,
                thumbnail = data["thumbnail"].asJsonObject["thumbnails"].asJsonArray
                    .toList()
                    .map { thumbnail -> thumbnail.asJsonObject["url"].asString },
                view = data["shortViewCountText"].asJsonObject["simpleText"].asString,
                published = data["publishedTimeText"].asJsonObject["simpleText"].asString
            )
        }
    }
}