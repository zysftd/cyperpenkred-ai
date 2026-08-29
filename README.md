# 赛博朋克红 AI - Cyberpunk Red AI

> 基于《赛博朋克红》核心规则的 AI 驱动桌面角色扮演 Android 应用

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-purple.svg)](https://developer.android.com/jetpack/compose)

## 什么是赛博朋克红 AI？

一款 Android 平台的赛博朋克红 AI 跑团应用。你作为唯一的真人玩家，与 AI 驱动的游戏主持人（GM）一起在夜之城展开冒险。

**除了你，所有人都是 AI。**

## 核心功能

### AI 游戏主持人
- 基于AI的智能 GM，能够推进剧情、扮演 NPC、判定骰点
- 112 条结构化规则知识库，AI 实时查询相关规则做判定
- 中文对话，赛博朋克风格叙事
- 规则+叙事平衡：严格基于规则判定，描述自由发挥

### 完整规则知识库
- 486 页规则书完整解析为 Wiki 格式
- 覆盖所有核心系统：角色创建、战斗、装备、义体、网行、世界设定
- 关键词匹配查询，AI 自动检索相关规则

### 角色创建系统
- 10 个职业：摇滚小子、佣兵、网行者、技工、技医、媒体人、主管、执法者、掮客、游民
- 完整生命路径系统：年龄、家庭、成长、直觉、外貌、个性、价值观、朋友、敌人、浪漫、命运
- 属性分配（10 属性）+ 技能分配（50+ 技能）
- 装备与义体选择

### 战斗系统
- 全自动骰点计算（1d10 技能判定、先攻、伤害）
- 远程/近战战斗规则完整实现
- 护甲 SP 削减、严重伤势、死亡豁免
- 战斗日志记录

### 义体系统
- 赛博组件完整列表（光学、音频、体内、外周、手臂、腿、脊椎）
- 人性值追踪，赛博精神病风险
- 义体安装/移除规则

### 网行系统
- 网络空间战斗模拟
- 交互界面能力、程序、黑冰
- 网络建筑探索

### 夜之城
- 14 个帮派详细数据
- 10 家巨型企业信息
- 关键地点与 NPC
- 节拍图表剧情推进

## 界面预览

| 首页 | 角色创建 | AI GM 对话 | 战斗 | 速查表 |
|------|----------|------------|------|--------|
| 快捷入口、最近对话 | 分步向导、属性分配 | 聊天界面、实时判定 | 先攻列表、伤害计算 | 武器/护甲/技能 |

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin（强类型、空安全、协程） |
| UI | Jetpack Compose + Material3 |
| 主题 | Dynamic Color (Android 12+) / 赛博朋克深色主题 |
| 依赖注入 | Hilt |
| 网络 | Retrofit + OkHttp |
| AI 后端 | OpenAI GPT-4 API |
| 本地存储 | Room + FTS4（关键词搜索） |
| 异步 | Kotlin Coroutines + Flow |

## 项目结构

```
app/src/main/java/com/cyperpunkred/ai/
├── data/
│   ├── local/db/          # Room 数据库、DAO、Entity
│   ├── remote/api/        # OpenAI API 接口
│   └── repository/        # 数据仓库
├── domain/
│   ├── model/             # 领域模型
│   ├── engine/            # 骰点/战斗/角色引擎
│   └── knowledge/         # 规则知识库查询引擎
├── di/                    # Hilt 模块
└── ui/
    ├── theme/             # 赛博朋克主题
    ├── navigation/        # 导航图
    ├── home/              # 首页
    ├── character/         # 角色管理
    ├── game/              # 游戏对话+战斗
    ├── quest/             # 任务追踪
    ├── rulebook/          # 速查表
    └── settings/          # 设置
```

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Kotlin 1.9+

### 安装步骤

1. **克隆仓库**
```bash
git clone https://github.com/zysftd/cyperpenkred-ai.git
cd cyperpenkred-ai
```

2. **用 Android Studio 打开项目**
```
File -> Open -> 选择项目目录
```

3. **同步 Gradle**
```
等待 Gradle 同步完成
```

4. **配置 API Key**
   - 启动应用后，进入「设置」页面
   - 输入你的 OpenAI API Key
   - 保存

5. **运行应用**
```
点击 Run 按钮，选择模拟器或真机
```

### 使用步骤

1. **创建角色**
   - 进入「角色」页面
   - 选择职业（如：佣兵）
   - 输入角色名称
   - 分配属性点（共 60 点）
   - 确认创建

2. **开始游戏**
   - 回到首页，点击「开始冒险」
   - AI GM 会发送欢迎消息
   - 描述你的角色在做什么
   - AI GM 会根据规则判定并推进剧情

3. **战斗**
   - 点击右上角战斗按钮
   - 查看先攻顺序和 HP
   - AI 自动计算骰点和伤害

4. **查阅规则**
   - 进入「速查」页面
   - 快速查看武器、护甲、技能、属性

## 规则知识库

应用内置 112 条结构化规则条目，覆盖：

| 类别 | 条目数 | 内容 |
|------|--------|------|
| 角色创建 | 12 | 属性、技能、生命路径、10 职业 |
| 战斗系统 | 8 | 先攻、动作、远程/近战、伤害、护甲 |
| 装备 | 8 | 武器表、护甲表、弹药、载具 |
| 义体系统 | 12 | 赛博精神病、全部组件（含花费和人性损失） |
| 网行系统 | 11 | 交互界面、程序、黑冰、网络建筑 |
| 世界设定 | 35 | 夜之城、帮派、企业、人物、地点 |
| GM 指南 | 17 | 节拍图表、遭遇表、大师课 |
| 日常生活 | 8 | 交通、医疗、药物、娱乐 |
| FAQ | 1 | 规则答疑 |

所有条目使用 Wiki Markdown 格式，包含表格、公式、数值。

## 核心规则速览

### 骰点判定
```
技能判定 = 1d10 + 属性值 + 技能等级 vs 目标值(DV)
先攻 = 1d10 + REF（反应）
伤害 = 武器伤害骰 - 护甲SP
死亡豁免 = 1d10 vs 体格值（HP<=0时触发）
```

### 战斗动作
| 动作 | 消耗 | 效果 |
|------|------|------|
| 射击 | 1 动作 | 远程攻击 |
| 近战攻击 | 1 动作 | 近身攻击 |
| 瞄准射击 | 2 动作 | 伤害 x2 |
| 闪避 | 1 反应 | 躲避攻击 |
| 掩护 | 1 动作 | 躲在掩体后 |

### 伤害计算
```
总伤害 = [武器伤害 x (出目 - DV)] - 护甲SP
爆头伤害 = 总伤害 x 2
范围攻击 = 仅削减身体护甲
```

## TODO

- [ ] 完整 PDF 解析脚本（自动提取全部 486 页）
- [ ] 多存档支持
- [ ] 角色导出/导入
- [ ] 战斗回放
- [ ] 多语言支持
- [ ] 离线 AI 模型支持

## 贡献

欢迎贡献代码、报告 Bug 或提出建议！

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 GNU General Public License v3.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 致谢

- [赛博朋克红](https://www.rtalstore.com/collections/cyberpunk-red) - R.Talsorian Games 的桌面角色扮演游戏
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Android 现代 UI 工具包
- [Hilt](https://dagger.dev/hilt/) - 依赖注入框架

## 联系方式

- GitHub: [@zysftd](https://github.com/zysftd)
- Issues: [GitHub Issues](https://github.com/zysftd/cyperpenkred-ai/issues)

---

**在夜之城，每个人都在为生存而战。你准备好了吗，choom？**
