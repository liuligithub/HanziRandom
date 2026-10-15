# 随机汉字抽取 App — 设计文档

日期：2026-10-15

## 目标

一个 iOS App，从一组汉字中每次随机抽取一个展示。点「下一个」从本轮剩余（未抽过）的汉字里再随机抽一个。用于识字/认字练习。

## 已确认的需求

- **字库来源**：预置一批默认汉字（硬编码），用户也可在 App 内添加/删除。
- **预置字库（31 个字）**：
  欢 给 哈 教 室 师 怎 惊 跳 歌 舞 找 做 故 担 拍 伸 拥 抓 寒 赛 温 湿 暖 冷 办 醒 被 对 杏
- **抽取行为**：一轮内不重复；每次打开 App 重新洗一轮。
- **抽完之后**：显示「全部抽完」，提供手动「重置」按钮，点了才重新开始。
- **持久化**：用户添加/删除的汉字保存在本地（UserDefaults），下次打开仍在；字号偏好也持久化。
- **朗读**：点击当前显示的大字，用中文语音（zh-CN）朗读；不自动朗读，可反复点。
- **展示增强**：
  - 显示进度：「第 X / 共 Y」。
  - 可调字号：滑块放大/缩小当前字的显示大小。

## 技术栈

- SwiftUI，iOS 16+
- `UserDefaults` 做持久化
- `AVSpeechSynthesizer` 做中文朗读
- 无第三方依赖

## 架构与组件

### CharacterStore（ObservableObject）— 核心数据与抽取逻辑

职责：管理全部汉字、当前轮次的抽取状态、持久化。

状态：
- `allCharacters: [String]` — 全部汉字（预置 + 用户增删）。持久化到 UserDefaults。
- `pool: [String]` — 当前轮次剩余可抽的汉字（内存状态，不持久化）。
- `current: String?` — 当前展示的汉字（内存状态）。
- `total: Int` — 本轮汉字总数（= 开始这轮时 allCharacters 的数量），用于进度显示。
- `drawnCount: Int` — 本轮已抽出的数量，用于进度显示「第 drawnCount / 共 total」。
- `isFinished: Bool` — pool 为空且已抽过（即全部抽完）。

方法：
- `drawNext()` — 从 pool 随机取一个移出，设为 current，drawnCount +1。pool 空则进入 finished 状态。
- `reset()` — 把 allCharacters 放回 pool（打乱），drawnCount 归零，重新抽第一个。
- `addCharacters(from text: String)` — 把输入文本拆成单字、过滤非汉字/空白、去重合并进 allCharacters，保存。
- `remove(_ character: String)` — 从 allCharacters 删除并保存。
- 首次运行（UserDefaults 无数据）用预置 31 字初始化。

边界与约定：
- 空字库：allCharacters 为空时，主页显示提示「请先在管理页添加汉字」，「下一个」不可用。
- 「一个字」= 一个 Unicode 字符（这里都是单个 CJK 汉字）。拆分按字符拆。
- 去重：合并时跳过已存在的字。

### SpeechService — 朗读封装

职责：封装 `AVSpeechSynthesizer`。
- `speak(_ text: String)` — 用 `AVSpeechSynthesisVoice(language: "zh-CN")` 朗读；每次朗读前停止上一次，避免排队重叠。

### 视图层

- **DrawView（主页/抽取页）**
  - 顶部：进度「第 X / 共 Y」。
  - 中间：大字显示 current，点击 → SpeechService.speak。
  - 字号滑块：调整当前字字号（偏好存 UserDefaults）。
  - 底部：「下一个」按钮；isFinished 时显示「全部抽完」文案 + 「重置」按钮。
  - 导航栏：进入 ManageView 的入口。
- **ManageView（管理页）**
  - 顶部：多行输入框，可批量粘贴一大段文字；「添加」按钮 → addCharacters。
  - 列表：展示 allCharacters，支持左滑删除（remove）。

## 数据流

- ManageView 修改 allCharacters → CharacterStore 保存 UserDefaults。
- DrawView 只读 allCharacters 填充 pool；reset / drawNext 操作内存中的 pool/current。
- current 变化 → DrawView 刷新；点字 → SpeechService 朗读。

## 错误/边界处理

- 空字库：禁用「下一个」，显示引导文案。
- 输入里含非汉字字符或重复字：addCharacters 自动过滤与去重，不报错。
- 朗读：中文语音不可用时静默失败（不崩溃）。

## 测试

对 CharacterStore 写单元测试（纯逻辑，可测）：
- drawNext 在一轮内不重复抽字。
- 抽到最后一个后 isFinished 为 true、pool 为空。
- reset 后 pool 恢复为全部汉字、drawnCount 归零。
- addCharacters 去重、过滤空白、合并正确。
- remove 正确删除。
- 进度计数（drawnCount / total）正确。

朗读（AVSpeechSynthesizer）涉及音频硬件，不做自动化测试，手动验证。

## 工程结构

项目建在 `~/HanziRandom`（独立目录，已初始化 git）。用 SwiftUI App 模板，`@main` App 持有一个 `CharacterStore` 作为 `@StateObject` 并以 `environmentObject` 下发。

## 不做的事（YAGNI）

- 不做拼音标注、笔顺动画（本次明确不要）。
- 不做跨设备同步、账号系统。
- 不做自动朗读。
