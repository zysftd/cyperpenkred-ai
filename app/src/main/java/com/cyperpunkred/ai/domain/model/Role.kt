package com.cyperpunkred.ai.domain.model

enum class Role(val displayName: String, val description: String) {
    ROCKERBOY("摇滚小子", "音乐与叛逆的力量"),
    SOLO("佣兵", "战斗与生存的专家"),
    NETRUNNER("网行者", "网络空间的黑客"),
    TECH("技工", "创造与改装的大师"),
    MEDTECH("技医", "医疗与义体专家"),
    MEDIA("媒体人", "真相的追寻者"),
    EXEC("主管", "权力与资源的掌控者"),
    LAWMAN("执法者", "法律的执行者"),
    FIXER("掮客", "人脉与交易的中间人"),
    NOMAD("游民", "荒野中的自由灵魂")
}
