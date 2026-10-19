package com.imcys.bilibilias.database.entity.download

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * `NamingConventionInfo` 与数据库 TEXT 列之间的编解码。
 *
 * 该逻辑原先在 `NamingConventionConverter`（`@TypeConverter`）里；为了移除对
 * `@TypeConverters` 的依赖（Room 3.0.x + KSP2 下该注解会导致 `[MissingType]`），
 * 改为普通对象，由实体上的 `@Ignore` 计算属性调用。
 *
 * **存储格式与改造前完全一致**：仍是同一个 JSON 结构、同样的键名与 null 处理，
 * 因此无需数据库迁移，历史数据可直接读取。
 */
object NamingConventionCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(value: String?): NamingConventionInfo? {
        if (value == null) return null
        val jsonObject = runCatching {
            json.parseToJsonElement(value).jsonObject
        }.getOrNull() ?: return null
        val ruleType = jsonObject["ruleType"]?.jsonPrimitive?.intOrNull
        return when (ruleType) {
            NamingConventionInfo.Video().ruleType -> NamingConventionInfo.Video(
                title = jsonObject.stringOrNull("title"),
                pTitle = jsonObject.stringOrNull("pTitle"),
                author = jsonObject.stringOrNull("author"),
                bvId = jsonObject.stringOrNull("bvId"),
                aid = jsonObject.stringOrNull("aid"),
                cid = jsonObject.stringOrNull("cid"),
                p = jsonObject.stringOrNull("p"),
                collectionTitle = jsonObject.stringOrNull("collectionTitle"),
                collectionSeasonTitle = jsonObject.stringOrNull("collectionSeasonTitle"),
            )

            NamingConventionInfo.Donghua().ruleType -> NamingConventionInfo.Donghua(
                title = jsonObject.stringOrNull("title"),
                episodeTitle = jsonObject.stringOrNull("episodeTitle"),
                episodeNumber = jsonObject.stringOrNull("episodeNumber"),
                cid = jsonObject.stringOrNull("cid"),
                seasonTitle = jsonObject.stringOrNull("seasonTitle"),
            )

            else -> null
        }
    }

    fun encode(info: NamingConventionInfo?): String? {
        info ?: return null
        return buildJsonObject {
            put("ruleType", JsonPrimitive(info.ruleType))
            when (info) {
                is NamingConventionInfo.Video -> {
                    putString("title", info.title)
                    putString("pTitle", info.pTitle)
                    putString("author", info.author)
                    putString("bvId", info.bvId)
                    putString("aid", info.aid)
                    putString("cid", info.cid)
                    putString("p", info.p)
                    putString("collectionTitle", info.collectionTitle)
                    putString("collectionSeasonTitle", info.collectionSeasonTitle)
                }

                is NamingConventionInfo.Donghua -> {
                    putString("title", info.title)
                    putString("episodeTitle", info.episodeTitle)
                    putString("episodeNumber", info.episodeNumber)
                    putString("cid", info.cid)
                    putString("seasonTitle", info.seasonTitle)
                }
            }
        }.toString()
    }
}

private fun JsonObject.stringOrNull(key: String): String? {
    val element = this[key] ?: return null
    return if (element is JsonNull) null else element.jsonPrimitive.content
}

private fun JsonObjectBuilder.putString(key: String, value: String?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
}
