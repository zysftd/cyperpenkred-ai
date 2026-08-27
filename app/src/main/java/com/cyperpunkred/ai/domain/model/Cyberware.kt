package com.cyperpunkred.ai.domain.model

data class Cyberware(
    val id: Long = 0,
    val name: String,
    val type: CyberwareType,
    val slot: String,
    val humanityCost: Int,
    val stats: Map<String, Int> = emptyMap(),
    val special: String? = null
)

enum class CyberwareType {
    CYBEROPTICS,      // 赛博光学
    CYBERAUDIO,       // 赛博音频
    CYBERINTERNAL,    // 体内组件
    CYBEREXTERNAL,    // 外周组件
    CYBERARM,         // 赛博手臂
    CYBERLEG,         // 赛博腿
    CYBERSPINE,       // 赛博脊椎
    CYBERSKULL,       // 赛博头骨
    CYBERHAND         // 赛博手
}
