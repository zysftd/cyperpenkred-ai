package com.cyperpunkred.ai.domain.knowledge

import com.cyperpunkred.ai.data.local.db.entity.RulebookEntryEntity
import com.cyperpunkred.ai.data.repository.RulebookRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulebookQueryEngine @Inject constructor(
    private val rulebookRepository: RulebookRepository
) {
    suspend fun queryByKeywords(keywords: List<String>, limit: Int = 5): List<RulebookEntryEntity> {
        return rulebookRepository.searchByKeywords(keywords, limit)
    }

    suspend fun getByCategory(category: String): List<RulebookEntryEntity> {
        return rulebookRepository.getEntriesByCategory(category)
    }

    suspend fun getEntry(id: Long): RulebookEntryEntity? {
        return rulebookRepository.getEntryById(id)
    }

    fun extractKeywords(text: String): List<String> {
        val keywordMap = mapOf(
            "射击" to listOf("射击", "远程", "枪", "手枪", "步枪", "霰弹枪"),
            "近战" to listOf("近战", "刀", "剑", "格斗", "搏击"),
            "伤害" to listOf("伤害", "HP", "生命值", "损伤"),
            "护甲" to listOf("护甲", "SP", "装甲", "防弹"),
            "骰点" to listOf("骰点", "判定", "检定", "投骰"),
            "先攻" to listOf("先攻", "回合", "行动顺序"),
            "义体" to listOf("义体", "赛博", "植入", "改造"),
            "网行" to listOf("网行", "网络", "黑客", "程序", "黑冰"),
            "技能" to listOf("技能", "能力", "专长"),
            "属性" to listOf("属性", "INT", "REF", "DEX", "TECH", "COOL")
        )

        val foundKeywords = mutableListOf<String>()
        val lowerText = text.lowercase()

        for ((category, synonyms) in keywordMap) {
            if (synonyms.any { lowerText.contains(it) }) {
                foundKeywords.add(category)
                foundKeywords.addAll(synonyms.filter { lowerText.contains(it) })
            }
        }

        return foundKeywords.distinct().take(10)
    }
}
