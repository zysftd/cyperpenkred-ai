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
            // 战斗相关
            "射击" to listOf("射击", "远程", "枪", "手枪", "步枪", "霰弹枪", "冲锋枪", "狙击", "开火"),
            "近战" to listOf("近战", "刀", "剑", "格斗", "搏击", "拳", "砍", "刺", "武士刀"),
            "伤害" to listOf("伤害", "HP", "生命值", "损伤", "伤害骰", "伤害计算", "扣血"),
            "护甲" to listOf("护甲", "SP", "装甲", "防弹", "凯夫拉", "合金装备", "阻滞力"),
            "骰点" to listOf("骰点", "判定", "检定", "投骰", "1d10", "2d6", "骰子"),
            "先攻" to listOf("先攻", "回合", "行动顺序", "先攻队列", "轮次"),
            "战斗动作" to listOf("动作", "闪避", "掩护", "瞄准", "装填", "逃跑", "攻击动作"),
            "暴击" to listOf("暴击", "爆头", "致命", "严重伤势", "大成功", "大失败"),
            "死亡" to listOf("死亡", "豁免", "濒死", "死亡豁免", "稳定伤势"),
            "范围攻击" to listOf("范围", "榴弹", "霰弹", "爆炸", "手雷", "火箭"),

            // 装备相关
            "武器" to listOf("武器", "枪械", "刀具", "装备", "军火"),
            "弹药" to listOf("弹药", "子弹", "弹夹", "弹匣", "穿甲弹", "燃烧弹", "空包弹"),
            "载具" to listOf("载具", "汽车", "摩托", "浮空车", "AV", "驾驶"),

            // 角色相关
            "属性" to listOf("属性", "INT", "REF", "DEX", "TECH", "COOL", "ATTR", "LUCK", "MA", "BODY", "EMP", "智力", "反应", "敏捷", "技术", "酷", "魅力", "运气", "移速", "体格", "共情"),
            "技能" to listOf("技能", "能力", "专长", "技能点", "运动", "搏击", "手枪", "长枪", "闪避", "潜行", "急救", "说服", "察言观色", "街头智慧"),
            "职业" to listOf("职业", "摇滚小子", "佣兵", "网行者", "技工", "技医", "媒体人", "主管", "执法者", "掮客", "游民"),
            "生命路径" to listOf("生命路径", "年龄", "家庭", "成长", "直觉", "外貌", "个性", "价值观", "朋友", "敌人", "浪漫", "命运", "人生目标"),

            // 义体相关
            "义体" to listOf("义体", "赛博", "植入", "改造", "赛博组件", "赛博光学", "赛博音频", "赛博手臂", "赛博腿", "赛博脊椎"),
            "赛博精神病" to listOf("赛博精神病", "人性", "人性值", "人性损失", "精神病", "反社会"),
            "义体安装" to listOf("安装", "插槽", "诊所", "购物中心", "医院", "花费", "价格"),

            // 网行相关
            "网行" to listOf("网行", "网络", "黑客", "程序", "黑冰", "赛博空间", "互联网", "META"),
            "黑冰" to listOf("黑冰", "防御程序", "攻击程序", "使魔", "R.A.B.I.D.S"),
            "网行设备" to listOf("赛博碟板", "虚像投影目镜", "交互接口", "交互界面"),

            // 世界相关
            "夜之城" to listOf("夜之城", "夜城", "NC", "NIGHT CITY"),
            "帮派" to listOf("帮派", "六街帮", "漩涡帮", "虎爪帮", "巫毒帮", "瓦伦蒂诺", "清道夫", "小丑帮"),
            "企业" to listOf("企业", "荒坂", "军用科技", "生物科技", "大陆品牌汇", "创伤小队", "巨企"),
            "创伤小组" to listOf("创伤小组", "创伤", "医疗", "白银级", "高管级", "医院"),

            // GM相关
            "GM" to listOf("GM", "主持", "游戏主持人", "守密者", "大师课"),
            "节拍" to listOf("节拍", "剧情", "故事", "开端", "发展", "悬念", "高潮", "解决"),
            "遭遇" to listOf("遭遇", "遭遇表", "随机遭遇", "事件"),
            "杂鱼" to listOf("杂鱼", "小兵", "NPC", "敌人", "对手"),

            // 日常生活
            "药物" to listOf("药物", "毒品", "兴奋剂", "镇定剂", "止痛药", "合成药"),
            "娱乐" to listOf("娱乐", "酒吧", "夜总会", "俱乐部", "派对"),
            "时尚" to listOf("时尚", "穿着", "装扮", "服装", "风格"),
            "住房" to listOf("住房", "公寓", "住宅", "安全屋", "生活风格")
        )

        val foundKeywords = mutableListOf<String>()
        val lowerText = text.lowercase()

        for ((category, synonyms) in keywordMap) {
            if (synonyms.any { lowerText.contains(it) }) {
                foundKeywords.add(category)
                foundKeywords.addAll(synonyms.filter { lowerText.contains(it) })
            }
        }

        return foundKeywords.distinct().take(15)
    }

    suspend fun getContextForAI(userMessage: String): String {
        val keywords = extractKeywords(userMessage)
        if (keywords.isEmpty()) return ""

        val entries = queryByKeywords(keywords, limit = 3)
        if (entries.isEmpty()) return ""

        return buildString {
            appendLine("## 相关规则参考")
            for (entry in entries) {
                appendLine("### ${entry.title}")
                appendLine(entry.content)
                appendLine()
            }
        }
    }
}
